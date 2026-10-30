# Spring Boot MVC – adresár zamestnancov

Aplikácia nad tabuľkou `employee` podľa oboch seminárnych prezentácií.

## Spustenie

1. Spustite MySQL (napr. v XAMPP).
2. Pre novú databázu importujte `rest/sql/schema.sql` cez phpMyAdmin. Ak už máte tabuľku z pôvodnej REST aplikácie s boolean stĺpcom `full_time`, exportujte zálohu a **namiesto schema.sql** spustite jednorazovo `rest/sql/migrate-full-time.sql`. Hodnota 1 sa zmení na Full-time, 0 na Part-time. Migráciu neopakujte nad novým textovým stĺpcom.
3. Nastavte pripojenie v `rest/src/main/resources/application.properties` (predvolene databáza `employee`, používateľ `root`, prázdne heslo).
4. V koreňovom priečinku spustite `start.cmd` alebo v priečinku `rest` spustite `mvnw.cmd spring-boot:run`. Projekt používa existujúce JDK 27.
5. Otvorte [http://localhost:8080](http://localhost:8080). Koreňová adresa presmeruje na `/employees`.

SQL skripty sa nespúšťajú automaticky. Aplikácia sama nemení existujúcu schému ani dáta.

## Trojvrstvová architektúra

- **Prezentačná:** `HomeController`, `EmployeeController` a `templates/employees/list.html`, `view.html`, `form.html`.
- **Aplikačná:** `EmployeeService` – jedna trieda s `@Service`, constructor injection a transakciami, podľa druhej prezentácie.
- **Dátová:** `EmployeeRepository extends JpaRepository<Employee, Integer>`, entita `Employee` mapuje tabuľku `employee`.

MVC Controller pristupuje k dátam iba cez Service. Pôvodné REST API na `/api/employees` používa rovnakú Service a Repository. Starý DAO balík zostáva zo staršieho zadania, nový MVC tok ho nepoužíva.

| Metóda | Adresa | Funkcia |
| --- | --- | --- |
| GET | `/employees` | Zoznam zamestnancov |
| GET | `/employees/{id}` | Detail všetkých údajov |
| GET | `/employees/create` | Prázdny formulár |
| POST | `/employees` | Vytvorenie zamestnanca |
| GET | `/employees/{id}/edit` | Predvyplnený formulár |
| POST | `/employees/{id}` | Uloženie úprav |
| POST | `/employees/{id}/delete` | Odstránenie po potvrdení |

Jedna šablóna formulára pomocou `employee.id` rozlišuje pridanie a úpravu. Používa `th:object`, `th:field`, `@ModelAttribute`, dynamické `th:action` a presmerovanie po úspešnej zmene. Formulár zobrazuje aj chyby validácie a zachováva vyplnené hodnoty. ID pri úprave sa preberá iba z URL.

## Pracovné pozície a úväzky

V `application.properties` sú dva zoznamy:

```properties
job-titles=Software Engineer,Data Analyst,Product Manager,Marketing Specialist,Project Manager,UX Designer,QA Engineer,System Administrator
full-time-options=Full-time,Part-time,Contract,Intern/Trainee
```

Controller ich načíta pomocou `@Value` do `List<String>` a vloží do modelu. Šablóna ich prechádza cez `th:each`: pozície v `<select>`, úväzky cez `<input type="radio">`. Možnosti sa neupravujú v HTML; po zmene properties reštartujte aplikáciu. Server overuje, že MVC formulár obsahuje platnú možnosť.

`Employee.fullTime` je teraz `String` a stĺpec `full_time` je `VARCHAR(30)`, pretože boolean nedokáže uchovať štyri typy úväzku. Aj REST API teraz očakáva napríklad `"fullTime": "Contract"`. `birthDate` je `LocalDate` (HTML dátum vo formáte `yyyy-MM-dd`, detail cez `#temporals.format`). Plat zostáva celočíselný ako v pôvodnom projekte.

`ZADANIE3.md` opisuje pôvodné REST zadanie pred touto zmenou typov.

## Overenie

V priečinku `rest` spustite `mvnw.cmd test`. Integračné testy používajú samostatnú pamäťovú H2 databázu; MySQL netreba spúšťať. Overujú vykreslené HTML, CRUD cez skutočné HTTP požiadavky, chyby formulára, dynamické možnosti a neexistujúce ID.
