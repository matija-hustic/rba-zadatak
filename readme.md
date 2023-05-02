# Zadatak

## Opis problema

Banka izdaje fizičkim osobama kreditne kartice. Osobe za to apliciraju banci. Za potrebe te evidencije treba napraviti mini-aplikaciju kojom će se evidentirati osoba(O) ili više njih predstavljenih Imenom, Prezimenom, OIB-om i Statusom za koje se treba izraditi kartica. Tip kartice nije bitan, već je samo jedan, tako da o tome ne trebaš razmišljati.<br>
Osobe se moraju zapisati permanentno,  a način izaberi sam po volji. Preferira se baza podataka (bilo koja, može i H2) ili datoteka.

Kako bi proces za proizvodnju/tiskanje kreditnih kartica znao čiju/koju karticu napraviti, treba mu dati tekstualnu datoteku(D) sa strukturom:<br>
Ime, Prezime, OIB, Status(delimiter izaberi sam, ne treba header).<br>
Napomena: Proces proizvodnje kartica ovdje je zamišljen da bi dao neki smisao, ali njime se nećeš baviti, nego samo ažuriranjem osoba i generiranjem tekstualne datoteke koju bi zamišljeni proces trebao čitati.

Aplikacija treba omogućiti:

- Upisivanje osobe(O) u skup osoba sa svim pripadajućim atributima(Ime, Prezime, OIB, Status),
- Pretraživanje skupa osoba(O) prema OIBu(ručni upis korisnika) osobe za koju želimo generirati datoteku(D), i ako osoba(O) postoji, vratiti Ime, Prezime, OIB i Status za istu; Inače ne vrati ništa, a može biti i neki exception da se zna što se desilo.
- Za pronađenu osobu(O) treba napraviti tekstualnu datoteku (D) sa svim popunjenim atributima(Ime, Prezime, OIB, Status).

Jedna datoteka(D) treba sadržavati podatke samo za jednu osobu(O).<br>
U nazivu datoteke možeš koristiti OIB + timestamp, radi lakše provjere da li datoteka već postoji.<br>
Lokacija spremanja datoteke nije bitna, može i u working direktorij aplikacije.<br>
Osoba(O) se treba moći obrisati na zahtjev prema OIBu(ručni upis korisnika).

Metode treba napraviti da rade kao RESTfull.<br>
Bonus Feature I:    Probaj se poigrati sa poljima Status (tip polja izaberi sam) u osobi(O) i/ili datoteci (D) imajući na umu da se podrži sljedeće:
- Jedna osoba(O) može imati samo jednu aktivnu datoteku(D)
- Ako obrišeš osobu(O),  datoteka treba biti označena kao neaktivna

Ne moraš raditi FrontEnd.<br>
Dovoljan je interface HTTP ili konzola, ili kako već odlučiš.<br>
Jezik je JAVA, a poželjno je korištenje frameworka, tipa spring boot i sl.

Štogod misliš da bi trebalo napraviti kao dodanu vrijednost, slobodno napravi, samo nam onda to napiši u mailu kad završiš, da možemo pogledati.<br>
Kad si gotov, možeš to na public GIT zakvačit pa pošalji link.

## Poslovna logika

Iz opisa problema naziru se ugrubo dva poslovna procesa.
- **Vodjenje evidencije** o osobama (korisnicima kartica), sto podrazumijeva unos i eventualnu naknadnu doradu podataka o osobama. Pri tome svakoj osobi osim njenih osnovnih podataka (ime, prezime, oib) pridruzen je podatak o stanju kartice (npr zatrazena, odobrena, dana u izdavanje, izdana, ponistena, ...) i aktivnosti korisnika kartice (`true` ako osoba koristi karticu / `false` ako osoba ne koristi karticu, ali nije zahtjevala brisanje podataka).
- **Izrada kartice** za osobe nakon sto su podaci o njima u konacnom obliku i spremni za izvoz putem datoteka u sustav za izradu kartica. Pri tome je moguce za danu osobu stvoriti vise datoteka, od kojih se samo posljednje stvorena smatra aktivnom, odnosno, od kojih se niti jedna ne smatra aktivnom ako je korisnik zatrazio brisanje svojih osobnih podataka.

Za evidenciju osoba zelim namijeniti REST resource (na backend serveru) kojem se pristupa preko putanje `/card-holders/{oib}`. Iza tog REST resourcea stoji tablica `card_holder` u bazi podataka.

Za izvoz podataka o osobama takodjer zelim namijeniti REST resource, na putanji `/card-holders/{oib}/exports`. Stvaranjem tog REST resourcea, stvara se i datoteka za osobu za koju se podaci izvoze. Namjera je da stvorenu datoteku preuzme vanjski sustav (npr za izradu kartica). Iz tog razloga jednom stvorena datoteka vise se ne smatra u nadleznosti aplikacije. Stoga aplikacija ne podrzava izmjenu sadrzaja ili brisanje datoteke, kao ni dohvat njenog sadrzaja. Medjutim, da bi se bilo u mogucnosti utvrditi koje datoteke su stvorene i koja od njih je aktivna, predvidjeno je vodjene evidencije o stvorenim datotekama. Za te potrebe u bazi podataka koristi se tablica `card_holder_export` za vodjenje zapisnika (log) o stvorenim datotekama.

## Rjesenje

Moj prijedlog rjesenja sastoji se od dvije komponente, **backend servera** razvijenog za OpenJDK (17.0.6) uz Spring Boot (3.0.6), i **baze podataka** PostgreSQL (15.2). Za potrebe demonstracije, komponente su konfigurirane da se pokrenu pomocu Docker Compose (2.17.2) unutar containera za Docker (20.10.24).
```sh
# pokretanje rjesenja
# - backend ce biti dostupan na localhost:${HOST_PORT_BACKEND},
#   gdje je HOST_PORT_BACKEND postavljen na 8080 u ./demo.env
# - postgresql baza podataka bit ce dostupna na localhost:${HOST_PORT_DB},
#   gdje je HOST_PORT_DB postavljen na 5432 u ./demo.env
docker-compose --file ./compose.demo.yaml --env-file ./demo.env up -d

# zaustavljanje rjesenja
docker-compose --file ./compose.demo.yaml down
```

Rjesenje je namijenjeno da mu se pristupa putem REST sucelja backend servera. Izravan pristup bazi podataka nije predvidjen, medjutim ipak je omogucen radi lakse provjere funkcioniranja rjesenja. Prema zahtjevima iz zadatka, backend server osim uzvratnih HTTP response poruka na REST sucelju, u odredjenim okolnostima takodjer izvozi podatke u izlazne datoteke na disku (uz prikladno namjesten mapping na Docker container).

Za potrebe testiranja rada rjesenja, nekoliko `curl` naredba:
```sh
HOST=localhost:8080

# add card holder, returns 201
curl -i -X POST http://${HOST}/card-holders \
  -H 'Content-Type: application/json' \
  -d '{"oib": 12345678901}'

# add with existing oib, returns 409
curl -i -X POST http://${HOST}/card-holders \
  -H 'Content-Type: application/json' \
  -d '{"oib": 12345678901}'

# add with invalid oib, returns 422
curl -i -X POST http://${HOST}/card-holders \
  -H 'Content-Type: application/json' \
  -d '{"oib": 123456789012}'

# update card holder, returns 200
curl -i -X PUT http://${HOST}/card-holders/12345678901 \
  -H 'Content-Type: application/json' \
  -d '{"oib": 12345678901, "cardStatus":"status10"}'

# update inexistent card hoder, returns 404
curl -i -X PUT http://${HOST}/card-holders/12345678902 \
  -H 'Content-Type: application/json' \
  -d '{"oib": 12345678902, "cardStatus":"status10"}'

# update with invalid card status, returns 422
curl -i -X PUT http://${HOST}/card-holders/12345678901 \
  -H 'Content-Type: application/json' \
  -d '{"oib": 12345678901, "cardStatus":"status11"}'

# get card holder, returns 200
curl -i -X GET http://${HOST}/card-holders/12345678901 \
  -H 'Content-Type: application/json'

# get inexistent card holder, returns 404
curl -i -X GET http://${HOST}/card-holders/12345678902 \
  -H 'Content-Type: application/json'

# export data for card holder, returns 201
curl -i -X POST http://${HOST}/card-holders/12345678901/exports \
  -H 'Content-Type: application/json'

# get exports for card holder, returns 200
curl -i -X GET http://${HOST}/card-holders/12345678901/exports \
  -H 'Content-Type: application/json'

# delete card holder, returns 200
curl -i -X DELETE http://${HOST}/card-holders/12345678901 \
  -H 'Content-Type: application/json'

# delete inexistent card holder, returns 404
curl -i -X DELETE http://${HOST}/card-holders/12345678901 \
  -H 'Content-Type: application/json'
```

## Development

Za razvoj sam koristio sljedece alate
- Svoj omiljeni code editor, VS Code (1.77.3) s default postavkama na Windows Windows 10 Pro (21H2, 19044.2846)
- git (2.39.2.windows.1) i njegov standardni command line terminal `git bash`
- [VS Code Coding Pack for Java](https://code.visualstudio.com/docs/java/java-tutorial#_coding-pack-for-java) koji sadrzi JDK (AdoptOpenJDK OpenJDK17U with HotSpot 17.0.6.+10 Eclipse Adoptium Temurin) i [Java extensions for Visual Studio Code](https://code.visualstudio.com/docs/java/extensions)

Koristeci Spring Initializr stvorio sam pocetni Java project uz parametre: Maven, Spring Boot 3.0.6, Java 17, Spring Data JPA, Spring Web. Nakon toga slijedio sam [tutorial na bezkoder.com](https://www.bezkoder.com/spring-boot-postgresql-example/) kako bih razvio funkcionirajuci backend server povezan s bazom podataka PostgreSQL.

Backend server pokrece se unutar VS Code naredbom `Debug: Start Debugging` (s postavkama u `./.vscode/launch.json`), a baza podataka pomocu Docker Compose:
```sh
# pokretanje baze podataka
docker compose --file ./compose.dev.yaml --env-file ./.env up -d

# zaustavljanje baze podataka
docker compose --file ./compose.dev.yaml down
```

## Tehnicke biljeske

Zamisljeni podatkovni model je kako slijedi
- osoba (`card_holder`)
  - id, autogenerated, primary key
  - first name, string
  - last name, string
  - oib, string
    - 11 digits
    - unique
    - immutable
  - card status, string
    - status1, ..., status10
    - null mapped to default value, status1
    - case insensitive matching
  - active, boolean
- datoteka (`card_holder_export`)
  - id, autogenerated, primary key
  - card holder id, foreign key
  - oib, string
  - timestamp, string
  - filename, string
    - **TODO** unique
  - active, boolean

Zamisljeno REST sucelje je kako slijedi
- post /card-holders (insert)
  - return 201, card holder object
  - return 409, conflict error message
  - return 422, validation error message
- put /card-holders/{oib} (update)
  - return 200, card holder object
  - return 404, not found error message
  - return 422, validation error message
- delete /card-holders/{oib} (delete)
  - return 204
  - return 404, not found error message
- get /card-holders/{oib} (select)
  - return 200, card holder object
  - return 404, not found error message
- post /card-holders/{oib}/exports
  - return 201
  - return 404, not found error message
- get /card-holders/{oib}/exports (select)
  - return 200, card holder export objects
  - return 404, not found error message
- general error handling (spring web)
  - return 405, method not allowed error message
  - return 500, unexpected error message

Pri razvoju aplikacije vodio sam se izmedju ostalog sljedecim nacelima
- prefer code maintainability over performance unless good reasons to do otherwise
  - code maintainability in the sense of ease of understanding, ease of change
  - performance in the sense of cpu and memory usage (also disk space, data bandwidth, ...)
  - good reasons being performance below acceptable tresholds for usability or cost-efficiency
- prefer data validation at frontend-backend interface over backend-database interface
  - unless implementation cost in fe-be interface is significantly higher than in be-db
  - data validation at fe-be interface usually unavoidable, while at be-db interface usually not really necessary<br>
    (fe-be interface public / external and not trusted, be-db interface private / internal and trusted)
  - concentrating data validation on the fe-be interface thus reduces duplication and separates concerns more
  - more specifically, keep orm usage and be-db interface generally minimal<br>
    use as few types and constraints in the database and orm as possible
- prefer more complex data manipulation to take place in backend rather than database
  - for example cascading data operations
  - code maintainability > performance gains
- use database primarily for data storage, less for data processing
  - separates concerns more between be and db
  - maximizing stateless parts of the system while minimizing stateful (functional vs imperative)
  - decreases reliance on database-specific features, keeps code base database-agnostic
- keep technical data internal, only business data external
  - for example object ids can be technical (for use in database joins) and business (for use in application logic)
  - instead of the two functions being fulfilled by a single id, it often makes sense to decouple the functions by having separate technical and business ids
  - db ids are usually technical data that should not propagate outside of backend
  - **TODO** for this reason use different objects on fe-be and be-db interfaces: DTOs and DBO, respectively
- respect rest conventions when defining endpoints
  - https://restfulapi.net/http-methods/
  - https://www.google.com/search?client=firefox-b-d&q=rest+methods
  - https://www.google.com/search?client=firefox-b-d&q=rest+put+method+with+query
  - https://stackoverflow.com/questions/611906/http-post-with-url-query-parameters-good-idea-or-not
- respect conventions for http responses on validation errors
  - https://stackoverflow.com/questions/3825990/http-response-code-for-post-when-resource-already-exists
  - https://stackoverflow.com/questions/3290182/which-status-code-should-i-use-for-failed-validations-or-invalid-duplicates
  - https://stackoverflow.com/questions/1959947/whats-an-appropriate-http-status-code-to-return-by-a-rest-api-service-for-a-val
  - **TODO** add verbose error messages to error http responses

Glavni dio rjesenja moze biti doradjen na mnogo nacina, a neke od znacajki koje nedostaju su
- **TODO** dodati transakcionalnost
  - https://www.baeldung.com/transaction-configuration-with-jpa-and-spring
  - https://vladmihalcea.com/spring-data-jpa-findbyid/
- **TODO** dodati testove
  - izbjegavati naknadno pisanje testova, prakticirati u sto vecoj mjeri test-driven razvoj
- **TODO** dodati dokumentaciju za REST API pomocu `springdoc-openapi`, https://www.baeldung.com/spring-rest-openapi-documentation

DevSecOps dio takodjer moze biti bolji
- **TODO** pristup REST resourceima pozeljno je ograniciti nekom od metoda autentikacije i autorizacije, npr pomocu cookies (https://auth0.com/docs/manage-users/cookies/spa-authenticate-with-cookies)
- **TODO** doraditi dockerfile, docker image mogao bi biti manji
- **TODO** idealno, rjesenje moze biti pokrenuto ne samo na lokalnom, vec i na udaljenom racunalu (https://www.docker.com/blog/how-to-deploy-on-remote-docker-hosts-with-docker-compose/), medjutim takav pristup otezan je potrebom za trajnom pohranom podataka
- **TODO** idealno, i razvoj se moze raditi na udaljenom racunalu, kako bi se izbjegla lokalna ogranicenja (CPU, RAM, HDD, inet bandwith, ...)
- **TODO** za slucaj potrebe veceg scalea, moze se koristiti cloud, kubernetes, terraform, helm, gitlab, alati od hashicorp, za potrebe arhitekture mikroservisa mogu biti korisni alati i metode za rad s distribuiranim podatkovnim modelom

## Radno vrijeme

|            |         |
| ---------- | ------- |
| 04/24/2023 | 6h      |
| 04/27/2023 | 5h      |
| 04/28/2023 | 8h      |
| 04/29/2023 | 7.5h    |
| 05/02/2023 | 2.5h    |
|            |         |
| **Ukupno** | **29h** |
