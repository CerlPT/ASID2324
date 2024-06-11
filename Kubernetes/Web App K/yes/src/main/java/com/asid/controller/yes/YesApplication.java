package com.asid.controller.yes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@SpringBootApplication
@RestController
public class YesApplication {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/students")
    public ResponseEntity<Object> handleStudentRequest(@RequestBody String jsonString) {
        try {
            // Extract parent and town information
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode jsonNodeMQ = objectMapper.readTree(jsonString);
            JsonNode parent = jsonNode.get("parent");
            JsonNode town = jsonNode.get("town");

            // Forward town to towns service
            String townsEndpoint = "http://towns:8080/towns"; 
            if (town != null) {
                // Replace country object with just the country name
                if (town.has("country")) {
                    ((ObjectNode) town).put("countryName", town.get("country").get("name").asText());
                    ((ObjectNode) town).remove("country");
                }

                HttpEntity<JsonNode> townRequest = new HttpEntity<>(town);
                try {
                    ResponseEntity<Object> townsResponse = restTemplate.exchange(townsEndpoint, HttpMethod.POST, townRequest, Object.class);
                    if (!townsResponse.getStatusCode().is2xxSuccessful()) {
                        return handleFailureAndDelete(jsonNode, 1);
                    }
                } catch (Exception e) {
                    return handleFailureAndDelete(jsonNode, 1);
                }

            }

            // Replace town in parent with townId
            if (parent != null) {
                if (parent.has("town")) {
                    ((ObjectNode) parent).put("townId", town.get("id").asInt());
                    ((ObjectNode) parent).remove("town");
                }

                // Forward parent to parents service
                String parentsEndpoint = "http://parents:8080/parents"; 
                HttpEntity<JsonNode> parentsRequest = new HttpEntity<>(parent);
                try {
                    ResponseEntity<Object> parentsResponse = restTemplate.exchange(parentsEndpoint, HttpMethod.POST, parentsRequest, Object.class);
                    if (!parentsResponse.getStatusCode().is2xxSuccessful()) {
                        return handleFailureAndDelete(jsonNode, 2);
                    }
                } catch (Exception e) {
                    return handleFailureAndDelete(jsonNode, 2);
                }
            }

            // Forward main json with updated IDs to students service
            ((ObjectNode) jsonNode).put("townId", town.get("id").asInt());
            ((ObjectNode) jsonNode).put("parentId", parent.get("id").asInt());
            ((ObjectNode) jsonNode).remove("town");
            ((ObjectNode) jsonNode).remove("parent");

            HttpEntity<JsonNode> studentRequest = new HttpEntity<>(jsonNode);
            String studentsEndpoint = "http://students:8080/students"; 
            try {
                ResponseEntity<Object> studentsResponse = restTemplate.exchange(studentsEndpoint, HttpMethod.POST, studentRequest, Object.class);
                if (!studentsResponse.getStatusCode().is2xxSuccessful()) {
                    return handleFailureAndDelete(jsonNode, 3);
                }
            } catch (Exception e) {
                handleFailureAndDelete(jsonNode, 3);
                return null;
            }
            sendMessageToQueue(jsonNodeMQ);

            return ResponseEntity.ok("Request successfully processed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Invalid JSON format or processing error: " + e.getMessage());
        }
    }

    @GetMapping("/students")
    public ResponseEntity<Object> getStudents() {
        try {
            // Discover service endpoints
            String studentsEndpoint = "http://students:8080/students"; 
            String parentsEndpoint = "http://parents:8080_URL/parents"; 
            String townsEndpoint = "http://towns:8080/towns"; 

            // Fetch data from services
            ResponseEntity<JsonNode> studentsResponse = restTemplate.exchange(studentsEndpoint, HttpMethod.GET, null, JsonNode.class);
            ResponseEntity<JsonNode> parentsResponse = restTemplate.exchange(parentsEndpoint, HttpMethod.GET, null, JsonNode.class);
            ResponseEntity<JsonNode> townsResponse = restTemplate.exchange(townsEndpoint, HttpMethod.GET, null, JsonNode.class);

            // Check if responses are successful
            if (studentsResponse.getStatusCode().is2xxSuccessful() && parentsResponse.getStatusCode().is2xxSuccessful() && townsResponse.getStatusCode().is2xxSuccessful()) {
                JsonNode studentsData = studentsResponse.getBody();
                JsonNode parentsData = parentsResponse.getBody();
                JsonNode townsData = townsResponse.getBody();

                // Combine data
                for (JsonNode student : studentsData) {
                    int parentId = student.get("parentId").asInt();
                    int townId = student.get("townId").asInt();

                    // Find parent and town by ID
                    JsonNode parent = StreamSupport.stream(parentsData.spliterator(), false)
                            .filter(p -> p.get("id").asInt() == parentId)
                            .findFirst()
                            .orElse(null);

                    JsonNode town = StreamSupport.stream(townsData.spliterator(), false)
                            .filter(t -> t.get("id").asInt() == townId)
                            .findFirst()
                            .orElse(null);

                    // Add parent and town data to student
                    ((ObjectNode) student).set("parent", parent);
                    ((ObjectNode) student).set("town", town);
                }

                return ResponseEntity.ok(studentsData);
            } else {
                return ResponseEntity.status(500).body("Failed to fetch data from one or more services.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error fetching data: " + e.getMessage());
        }
    }
    
    @GetMapping("/population")
    public ResponseEntity<Object> getPopulationData(@RequestParam(name = "minPopulation", required = false, defaultValue = "0") int minPopulation) {
        try {
            // Discover the population service endpoint
            String populationEndpoint = "http://population:8080/population"; 

            // Make a GET request to the population service
            ResponseEntity<JsonNode> response = restTemplate.exchange(populationEndpoint, HttpMethod.GET, null, JsonNode.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode populationData = response.getBody();
                // Filter the population data based on the minPopulation parameter
                List<JsonNode> filteredPopulationData = StreamSupport.stream(populationData.spliterator(), false)
                        .filter(node -> node.get("townPopulation").asInt() > minPopulation)
                        .collect(Collectors.toList());

                // Convert the filtered list back to a JsonNode
                JsonNode filteredJsonNode = objectMapper.valueToTree(filteredPopulationData);

                return ResponseEntity.ok(filteredJsonNode);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Failed to fetch population data.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error fetching population data: " + e.getMessage());
        }
    }

    @PostMapping("/clubs")
    public ResponseEntity<Object> associateClubToStudent(@RequestParam int studentId, @RequestBody JsonNode clubInfo) {
        try {
            // Fetch the list of students
            String studentsEndpoint = "http://students:8080/students"; 
            ResponseEntity<JsonNode> studentsResponse = restTemplate.exchange(studentsEndpoint, HttpMethod.GET, null, JsonNode.class);
    
            if (studentsResponse.getStatusCode().is2xxSuccessful()) {
                JsonNode studentsData = studentsResponse.getBody();
                
                // Find the student with the provided studentId
                JsonNode student = StreamSupport.stream(studentsData.spliterator(), false)
                        .filter(s -> s.get("id").asInt() == studentId)
                        .findFirst()
                        .orElse(null);
    
                if (student == null) {
                    return ResponseEntity.status(404).body("Student not found");
                }
    
                // Send the club information to the clubs service
                String clubsEndpoint = "http://clubs:8080/clubs"; 
                HttpEntity<JsonNode> clubRequest = new HttpEntity<>(clubInfo);
                ResponseEntity<Object> clubsResponse = restTemplate.exchange(clubsEndpoint, HttpMethod.POST, clubRequest, Object.class);
    
                if (!clubsResponse.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(500).body("Failed to send club information");
                }
    
                // Add clubId to the student JSON
                ((ObjectNode) student).put("clubId", clubInfo.get("id").asInt());
    
                // Send the updated student information to the students service
                HttpEntity<JsonNode> updateStudentRequest = new HttpEntity<>(student);
                ResponseEntity<Object> updateStudentResponse = restTemplate.exchange(studentsEndpoint, HttpMethod.POST, updateStudentRequest, Object.class);
    
                if (!updateStudentResponse.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(500).body("Failed to update student information");
                }
                
                System.out.println(student);
                return ResponseEntity.ok("Club associated with student successfully");
            } else {
                return ResponseEntity.status(500).body("Failed to fetch students");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }
    

    private void sendMessageToQueue(JsonNode jsonNodeMQ) {
        try {
            // Extract necessary fields for the RabbitMQ message
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("id", jsonNodeMQ.get("id").asInt());
            messageNode.put("firstName", jsonNodeMQ.get("firstName").asText());
            messageNode.put("lastName", jsonNodeMQ.get("lastName").asText());
            if (jsonNodeMQ.has("town")) {
                JsonNode town = jsonNodeMQ.get("town");
                if (town.has("popSize")) {
                    messageNode.put("townPopulation", town.get("popSize").asInt());
                }
            }

            // Connect to RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost"); // RabbitMQ server host
            factory.setUsername("guest");
            factory.setPassword("guest");
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                // Declare a queue
                channel.queueDeclare("view-queue", true, false, false, null);

                // Create message body
                String message = messageNode.toString();

                // Publish the message to the queue
                channel.basicPublish("", "view-queue", null, message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private ResponseEntity<Object>handleFailureAndDelete(JsonNode jsonNode, int level) {
        try {
            switch (level) {
                case 1:
                    return ResponseEntity.ok("Deletion stopped at level 1");
                case 2:
                    // Send delete request to towns service
                    int townIdLevel2 = jsonNode.get("townId").asInt();
                    String townsEndpointLevel2 = "http://towns:8080/towns/" + townIdLevel2; 
                    ResponseEntity<Object> deleteTownsResponseLevel2 = restTemplate.exchange(townsEndpointLevel2, HttpMethod.DELETE, null, Object.class);
                    if (!deleteTownsResponseLevel2.getStatusCode().is2xxSuccessful()) {
                        return ResponseEntity.status(deleteTownsResponseLevel2.getStatusCode()).body("Failed to delete towns");
                    }
                    return ResponseEntity.ok("Deletion stopped at level 2");
                case 3:
                    // Send delete request to towns service
                    int townIdLevel3 = jsonNode.get("townId").asInt();
                    String townsEndpointLevel3 = "http://towns:8080/towns/" + townIdLevel3; 
                    ResponseEntity<Object> deleteTownsResponseLevel3 = restTemplate.exchange(townsEndpointLevel3, HttpMethod.DELETE, null, Object.class);
                    if (!deleteTownsResponseLevel3.getStatusCode().is2xxSuccessful()) {
                        return ResponseEntity.status(deleteTownsResponseLevel3.getStatusCode()).body("Failed to delete towns");
                    }

                    // Send delete request to parents service
                    int parentIdLevel3 = jsonNode.get("parentId").asInt();
                    String parentsEndpointLevel3 = "http://parents:8080/parents/" + parentIdLevel3; 
                    ResponseEntity<Object> deleteParentsResponse = restTemplate.exchange(parentsEndpointLevel3, HttpMethod.DELETE, null, Object.class);
                    if (!deleteParentsResponse.getStatusCode().is2xxSuccessful()) {
                        // If parent deletion fails, delete town and return error
                        restTemplate.exchange(townsEndpointLevel3, HttpMethod.DELETE, null, Object.class);
                        return ResponseEntity.status(deleteParentsResponse.getStatusCode()).body("Failed to delete parent. Town also deleted.");
                    }

                    return ResponseEntity.ok("Deletion completed at level 3");
                default:
                    return ResponseEntity.badRequest().body("Invalid deletion level");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public static void main(String[] args) {
        SpringApplication.run(YesApplication.class, args);
    }
}