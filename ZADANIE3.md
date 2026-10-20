# Zadanie 3 – Adresár zamestnancov

## Čo je doplnené

| Metóda | URL | Výsledok |
| --- | --- | --- |
| GET | `/api/employees` | Všetci zamestnanci, HTTP 200 |
| GET | `/api/employees/{id}` | Jeden zamestnanec, HTTP 200 |
| POST | `/api/employees` | Nový zamestnanec s vygenerovaným ID, HTTP 201 a hlavička Location |
| PUT | `/api/employees/{id}` | Aktualizovaný zamestnanec, HTTP 200 |
| DELETE | `/api/employees/{id}` | Potvrdenie vymazania zamestnanca, HTTP 200 |

Každý úspešný endpoint vracia JSON s poľami `message` (informačná správa)
a `data` (výsledné údaje). Pri GET zoznamu je `data` pole zamestnancov, pri
GET podľa ID, POST a PUT objekt zamestnanca a pri DELETE ID zmazaného zamestnanca.
Napríklad odpoveď na úspešné vymazanie ID 5:

```json
{
  "message": "Zamestnanec s ID 5 bol úspešne zmazaný.",
  "data": 5
}
```

DELETE používa stav 200, pretože stav 204 nepovoľuje telo odpovede.
POST naďalej vracia stav 201 a hlavičku Location; ID nového záznamu je v `data.id`.
Správu Controller vytvorí až po úspešnom dokončení operácie v Service vrstve.

GET podľa ID, PUT a DELETE vrátia **404 Not Found**, ak záznam neexistuje.
Nečíselné ID a neplatný JSON vrátia **400 Bad Request**.

## Ako sú vrstvy prepojené

```text
REST Client (HTTP požiadavka s JSON)
    ↓
EmployeeRestController – URL, HTTP metóda, telo požiadavky a HTTP odpoveď
    ↓
EmployeeService – rozhranie operácií
    ↓
EmployeeServiceImpl – vykonanie operácie, kontrola existencie, transakcie
    ↓
EmployeeRepository extends JpaRepository<Employee, Integer>
    ↓
JPA / Hibernate – preklad operácií nad objektmi na SQL
    ↓
MySQL – databáza employee, tabuľka employee
```

Výsledok sa vracia späť cez Service do Controlleru. Spring prevedie objekt
`ApiResponse` na JSON, ktorý vidí REST Client. Jeho pole `data` obsahuje
zamestnanca, zoznam zamestnancov alebo zmazané ID a pole `message` obsahuje správu.

### 1. Entita Employee

Súbor `rest/src/main/java/com/example/rest/entity/Employee.java` opisuje zamestnanca.
`@Entity` označuje triedu ako JPA entitu. `@Table(name = "employee")` ju prepája
s databázovou tabuľkou a `@Column` prepája vlastnosti so stĺpcami, napríklad
`firstName` so stĺpcom `first_name`. JSON používa názvy vlastností ako `firstName`.

`@Id` označuje primárny kľúč typu `Integer` a
`@GeneratedValue(strategy = GenerationType.IDENTITY)` znamená, že nové ID
vygeneruje databáza. Stĺpec ID musí byť v MySQL nastavený ako AUTO_INCREMENT.

### 2. Repository

Súbor `rest/src/main/java/com/example/rest/repository/EmployeeRepository.java`:

```java
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
}
```

`Employee` určuje typ entity a `Integer` typ jej ID. Spring Data JPA pri štarte
vytvorí implementáciu tohto rozhrania. Preto nemusíme písať vlastné SQL ani
implementačnú triedu. Zdedené metódy sú napríklad `findAll()`, `findById(id)`,
`save(employee)` a `deleteById(id)`.

Pôvodné `EmployeeDAO` a `EmployeeDAOJpaImpl` sú nahradené týmto rozhraním.

### 3. Service

`EmployeeService.java` je rozhranie, ktoré stanovuje dostupné operácie.
`EmployeeServiceImpl.java` ich implementuje a volá Repository.
`@Service` umožňuje Springu vytvoriť a spravovať objekt tejto triedy.

- `findAll()` zavolá repository `findAll()`.
- `findById(id)` dostane `Optional<Employee>`. Ak obsahuje zamestnanca, vráti ho;
  ak je prázdny, vyhodí `ResponseStatusException` so stavom 404.
- `create(employee)` nastaví ID na `null` a zavolá `save()`. Tým vytvorí nový
  záznam aj vtedy, keď klient omylom pošle existujúce ID.
- `update(id, employee)` najprv načíta existujúci záznam, prepíše všetkých osem
  údajových polí a zavolá `save()`. Rozhoduje ID z URL; ID v JSON sa nepoužíva.
- `deleteById(id)` najprv overí existenciu záznamu a potom zavolá
  repository `deleteById(id)`.

`PUT` nahrádza všetky uvedené údaje. Posielaj preto celý objekt; vynechané
polia sa nastavia na `null`. Neexistujúce ID sa pri PUT nevytvára.

`@Transactional(readOnly = true)` nastavuje čítacie metódy ako čítacie transakcie.
Metódy vytvárania, aktualizácie a mazania majú vlastné `@Transactional`, ktoré
povoľuje zápis. Zápis sa potvrdí pri úspešnom dokončení transakcie; pri bežnej
runtime výnimke sa zmeny vrátia späť.

### 4. Controller

Súbor `rest/src/main/java/com/example/rest/controller/EmployeeRestController.java`
prijíma HTTP požiadavky. `@RestController` zabezpečuje vracanie dát v tele odpovede
a `@RequestMapping("/api")` určuje spoločný prefix URL.

`@GetMapping`, `@PostMapping`, `@PutMapping` a `@DeleteMapping` prepájajú HTTP
metódy a URL s konkrétnymi Java metódami. `@PathVariable("id")` prečíta ID z URL.
`@RequestBody` prevedie JSON z tela požiadavky na objekt `Employee`.
`ResponseEntity` umožňuje nastaviť stav odpovede, hlavičky aj telo.
Trieda `dto/ApiResponse.java` určuje spoločný formát odpovede. Generický typ `T`
umožňuje vložiť do poľa `data` rôzne typy údajov. Správy sa pridávajú v Controlleri;
Service a Repository naďalej pracujú so zamestnancami.

Controller dostane `EmployeeService` cez konštruktor. Spring mu poskytne objekt
`EmployeeServiceImpl`, ktorému zase poskytne implementáciu `EmployeeRepository`.
Toto sa nazýva dependency injection: objekty netreba vytvárať ručne cez `new`.
Skenovanie tried zabezpečuje hlavná trieda `RestApplication` s `@SpringBootApplication`.

## Konkrétny priebeh PUT /api/employees/5

1. REST Client odošle PUT s celým zamestnancom v JSON.
2. Spring vyberie `updateEmployee()`, ID 5 vloží do argumentu `id` a JSON prevedie
   na objekt `Employee`.
3. Controller zavolá `employeeService.update(5, employee)`.
4. Service zavolá `repository.findById(5)`. Ak záznam neexistuje, odpoveď bude 404.
5. Service prepíše údaje načítaného zamestnanca a zavolá `repository.save()`.
6. Hibernate zabezpečí aktualizáciu databázy v transakcii.
7. Controller vráti JSON so stavom 200, správou v `message` a aktualizovaným
   zamestnancom v `data`.

## Spustenie a REST Client

Projekt je v podpriečinku `rest`. Podľa existujúceho `pom.xml` používa JDK 27.
Musí bežať MySQL s databázou `employee` a existujúcou tabuľkou `employee`, ktorá
zodpovedá entite. Pripojenie je v `rest/src/main/resources/application.properties`:
`jdbc:mysql://localhost:3306/employee`, používateľ `root`, pôvodne prázdne heslo.
Prispôsob ho svojej lokálnej databáze. Aplikácia aktuálne nemá nastavené automatické
vytváranie schémy.

V IntelliJ IDEA nastav JDK 27 a spusti `RestApplication.main()`. Alternatívne
z podpriečinka `rest` spusti `mvn spring-boot:run`, ak máš Maven a JDK v PATH.
Štandardná adresa je `http://localhost:8080`.

Požiadavky môžeš odosielať cez REST Client alebo Swagger UI na
`http://localhost:8080/swagger-ui/index.html`. Najprv odošli POST a prečítaj
vygenerované ID z poľa `data.id`. Použi ho v URL pri GET, PUT a DELETE.
Po vymazaní má GET rovnakého ID vrátiť 404.

