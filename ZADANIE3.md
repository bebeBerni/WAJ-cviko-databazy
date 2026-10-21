# Zadanie 3 – Adresár zamestnancov

## Čo je doplnené

| Metóda | URL | Výsledok |
| --- | --- | --- |
| GET | `/api/employees` | Všetci zamestnanci, HTTP 200 |
| GET | `/api/employees/{id}` | Jeden zamestnanec, HTTP 200 |
| POST | `/api/employees` | Nový zamestnanec s vygenerovaným ID, HTTP 201 a hlavička Location |
| PUT | `/api/employees/{id}` | Aktualizovaný zamestnanec, HTTP 200 |
| DELETE | `/api/employees/{id}` | Potvrdenie vymazania zamestnanca, HTTP 200 |

Každý endpoint pri úspechu aj chybe vracia JSON s presne štyrmi položkami:
`data` (výsledné údaje alebo `null` pri chybe), `message` (správa),
`errors` (objekt chýb podľa polí, pri úspechu `{}`) a `timestamp`
(`java.time.Instant`, aktuálny čas vytvorenia odpovede na serveri v ISO-8601 UTC).
Pri GET zoznamu je `data` pole zamestnancov, pri
GET podľa ID, POST a PUT objekt zamestnanca a pri DELETE ID zmazaného zamestnanca.
Napríklad odpoveď na úspešné vymazanie ID 5:

```json
{
  "message": "Zamestnanec s ID 5 bol úspešne zmazaný.",
  "data": 5,
  "errors": {},
  "timestamp": "2026-10-21T07:45:00.123Z"
}
```

DELETE používa stav 200, pretože stav 204 nepovoľuje telo odpovede.
POST naďalej vracia stav 201 a hlavičku Location; ID nového záznamu je v `data.id`.
Správu Controller vytvorí až po úspešnom dokončení operácie v Service vrstve.

GET podľa ID, PUT a DELETE vrátia **404 Not Found**, ak záznam neexistuje.
Nečíselné ID a neplatný JSON vrátia **400 Bad Request**.

## Validácia vstupných dát

Dependency `spring-boot-starter-validation` v `rest/pom.xml` poskytuje Jakarta
Bean Validation. `@Valid` na telách POST a PUT spúšťa kontrolu ešte pred volaním
Service. `@Positive` kontroluje ID z URL pri GET, PUT a DELETE.

| Pole | Pravidlá |
| --- | --- |
| `firstName`, `lastName` | Povinné, neprázdne, najviac 100 znakov |
| `birthDate` | Povinný dátum v minulosti |
| `email` | Povinný, najviac 254 znakov, `@Email` aj vlastný `@Pattern` |
| `phone` | Povinný, 7–15 číslic, spolu najviac 25 znakov; povolené medzery, zátvorky, pomlčky a úvodné `+` |
| `jobTitle` | Povinná, neprázdna, najviac 150 znakov |
| `salary` | Povinný, nezáporný; nula je povolená |
| `fullTime` | Povinný boolean; `false` je platná hodnota |

Emailový regulárny výraz podporuje bežný ASCII formát „dot-atom“ podľa
[RFC 5321, § 4.1.2 a § 4.5.3.1](https://www.rfc-editor.org/rfc/rfc5321.html)
a [RFC 5322, § 3.2.3](https://www.rfc-editor.org/rfc/rfc5322.html).
Pred `@` povoľuje aj znaky ako `+` a apostrof, maximálne 64 ASCII znakov;
bodka nesmie byť prvá, posledná ani zdvojená. Doména musí obsahovať bodku,
každá jej časť má 1–63 znakov, len písmená, číslice a vnútorné pomlčky.
Nepoužíva sa zastaraný limit 2–6 znakov na koncovku domény. Podporované sú aj
IDN domény zapísané cez punycode, napríklad `user@xn--bcher-kva.de`.

Je to zámerne validácia bežného formátu, nie úplný parser všetkých RFC adries:
adresy s úvodzovkami, IP literálom či priamym Unicode lokálnym menom sú mimo
rozsahu API. Unicode lokálne mená vyžadujú rozšírenie SMTPUTF8 podľa
[RFC 6531](https://www.rfc-editor.org/rfc/rfc6531.html). Kontrola formátu
neoveruje existenciu domény ani doručiteľnosť schránky.

`GlobalExceptionHandler` (`@RestControllerAdvice`) zjednocuje chyby do `ApiResponse`:

| Stav | Situácia |
| --- | --- |
| 400 | Neplatné polia, nekladné/nečíselné ID, chýbajúce telo alebo neplatný JSON |
| 404 | Neexistujúci zamestnanec alebo cesta |
| 405 | Nepovolená HTTP metóda; zachová sa hlavička `Allow` |
| 406 | Klient požaduje nepodporovaný formát odpovede |
| 409 | Konflikt s databázovými obmedzeniami |
| 415 | Nepodporovaný typ obsahu požiadavky |
| 500 | Neočakávaná chyba; detaily sa zapisujú do logu servera |

Príklad HTTP 400 pri chybnom emaile (obsah `errors.email` môže obsahovať
viac správ, pretože sa kontroluje `@Email` aj `@Pattern`):

```json
{
  "data": null,
  "message": "Vstupné údaje nie sú platné.",
  "errors": {
    "email": ["Email musí mať bežný formát meno@domena.sk."]
  },
  "timestamp": "2026-10-21T07:45:00.123Z"
}
```

Čas sa pri každej odpovedi vytvára nanovo cez `Instant.now()`. Aj pri chybe
ostávajú všetky štyri položky prítomné a typ odpovede je `application/json`.

Automatické testy boli na požiadanie odstránené. API možno overiť cez REST klienta:
GET `/api/employees` má vrátiť 200, POST s telom `{}` a typom obsahu
`application/json` má vrátiť 400 s validačnými chybami. Obe odpovede musia
obsahovať `data`, `message`, `errors` a `timestamp`.

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
povinné polia spôsobia validačnú chybu 400. Neexistujúce ID sa pri PUT nevytvára.

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
`@RequestBody` prevedie JSON z tela požiadavky na objekt `Employee` a `@Valid`
skontroluje jeho validačné anotácie.
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

Najjednoduchšie spustenie vo Windows: zapni **MySQL** v XAMPP a v koreňovom
priečinku projektu spusti `start.cmd` (v PowerShelli `./start.cmd`). Skript
vyberie existujúce JDK 27 z `%USERPROFILE%/.jdks/openjdk-27` a spustí Maven
wrapper. Ak máš JDK inde, nastav `JAVA_HOME` na jeho umiestnenie.

V IntelliJ IDEA nastav JDK 27 a spusti `RestApplication.main()`. Alternatívne
z podpriečinka `rest` spusti `mvn spring-boot:run`, ak máš Maven a JDK v PATH.
Štandardná adresa je `http://localhost:8080`.

Požiadavky môžeš odosielať cez REST Client alebo Swagger UI na
`http://localhost:8080/swagger-ui/index.html`. Najprv odošli POST a prečítaj
vygenerované ID z poľa `data.id`. Použi ho v URL pri GET, PUT a DELETE.
Po vymazaní má GET rovnakého ID vrátiť 404.

