package com.example.rest;

import com.example.rest.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:employees;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Hodnota navyse dokazuje, ze formular cerpa moznosti z properties.
        "job-titles=Software Engineer,Data Analyst,Product Manager,Marketing Specialist,Project Manager,UX Designer,QA Engineer,System Administrator,Test Position"
})
class EmployeeMvcTest {
    @LocalServerPort
    private int port;

    @Autowired
    private EmployeeRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void rendersEmptyListAndConfiguredOptions() throws Exception {
        assertRedirect(get("/"));
        assertThat(get("/employees").body()).contains("Zatiaľ nebol pridaný");
        HttpResponse<String> form = get("/employees/create");
        assertThat(form.statusCode()).isEqualTo(200);
        assertThat(form.body()).contains("<select", "Test Position", "Software Engineer", "Data Analyst",
                "Product Manager", "Marketing Specialist", "Project Manager", "UX Designer",
                "QA Engineer", "System Administrator", "Full-time", "Part-time", "Contract", "Intern/Trainee");
        assertThat(form.body().split("type=\"radio\"", -1)).hasSize(5);
        assertThat(get("/css/style.css").statusCode()).isEqualTo(200);
    }

    @Test
    void createsViewsUpdatesAndDeletesEmployee() throws Exception {
        Map<String, String> data = validData();
        data.put("id", "123456"); // Formular nesmie urcovat ID noveho zaznamu.
        assertRedirect(post("/employees", data));
        var employee = repository.findAll().getFirst();
        int id = employee.getId();
        assertThat(id).isNotEqualTo(123456);
        assertThat(employee.getFullTime()).isEqualTo("Contract");
        assertThat(get("/employees").body()).contains("Jana", "Nováková", "/employees/" + id);
        HttpResponse<String> detail = get("/employees/" + id);
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body()).contains("15.05.1995", "2 500,00", "Contract", "jana@example.sk");
        String edit = get("/employees/" + id + "/edit").body();
        assertThat(edit).contains("Upraviť zamestnanca", "value=\"Jana\"", "value=\"1995-05-15\"",
                "selected=\"selected\"", "checked=\"checked\"", "action=\"/employees/" + id + "\"");
        data.put("firstName", "Eva");
        data.put("fullTime", "Intern/Trainee");
        assertRedirect(post("/employees/" + id, data));
        assertThat(repository.findById(id).orElseThrow().getFirstName()).isEqualTo("Eva");
        assertThat(repository.findById(id).orElseThrow().getFullTime()).isEqualTo("Intern/Trainee");
        assertThat(repository.count()).isEqualTo(1);
        assertRedirect(post("/employees/" + id + "/delete", Map.of()));
        assertThat(repository.count()).isZero();
        assertThat(get("/employees/" + id).statusCode()).isEqualTo(404);
    }

    @Test
    void supportsAllFourEmploymentTypes() throws Exception {
        for (String type : new String[]{"Full-time", "Part-time", "Contract", "Intern/Trainee"}) {
            var data = validData();
            data.put("fullTime", type);
            assertRedirect(post("/employees", data));
        }
        assertThat(repository.findAll()).extracting("fullTime")
                .containsExactlyInAnyOrder("Full-time", "Part-time", "Contract", "Intern/Trainee");
    }

    @Test
    void invalidFormsKeepOptionsAndDoNotWriteToDatabase() throws Exception {
        var data = validData();
        data.put("firstName", "");
        data.put("jobTitle", "Unknown job");
        data.put("fullTime", "Unknown type");
        data.put("salary", "abc");
        data.put("birthDate", "invalid-date");
        HttpResponse<String> response = post("/employees", data);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Meno je povinné.", "Vyberte pozíciu zo zoznamu.",
                "Vyberte platný typ úväzku.", "Test Position", "Intern/Trainee", "jana@example.sk");
        assertThat(repository.count()).isZero();

        assertRedirect(post("/employees", validData()));
        int id = repository.findAll().getFirst().getId();
        response = post("/employees/" + id, data);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("action=\"/employees/" + id + "\"", "Uložiť zmeny");
        assertThat(repository.findById(id).orElseThrow().getFirstName()).isEqualTo("Jana");
    }

    @Test
    void missingEmployeeReturnsHtml404AndCannotBeUpdatedOrDeleted() throws Exception {
        HttpResponse<String> response = get("/employees/99999");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Chyba 404", "Späť na zoznam");
        assertThat(get("/employees/99999/edit").statusCode()).isEqualTo(404);
        assertThat(post("/employees/99999", validData()).statusCode()).isEqualTo(404);
        assertThat(post("/employees/99999/delete", Map.of()).statusCode()).isEqualTo(404);
        assertThat(repository.count()).isZero();
    }

    private Map<String, String> validData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("firstName", "Jana");
        data.put("lastName", "Nováková");
        data.put("birthDate", "1995-05-15");
        data.put("email", "jana@example.sk");
        data.put("phone", "+421900123456");
        data.put("jobTitle", "Software Engineer");
        data.put("salary", "2500");
        data.put("fullTime", "Contract");
        return data;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Accept", "text/html").GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, Map<String, String> data) throws Exception {
        String body = data.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(uri(path)).header("Accept", "text/html")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void assertRedirect(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElseThrow()).endsWith("/employees");
    }
}
