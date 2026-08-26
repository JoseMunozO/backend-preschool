# Teknisk översikt (svenska)

Det här dokumentet förklarar hur backend-systemet fungerar i stora drag: varför vi valde den
säkerhetsmetod vi använder, hur roller hanteras, vilka buggar vi stötte på och varför, samt vad
Docker faktiskt gör här. Skrivet för att vara lätt att förklara muntligt, inte som en fullständig
kodgenomgång.

## 1. Vad appen gör

Det är ett administrationssystem för en förskola: elever, föräldrar/vårdnadshavare, månatliga
betalningar, material/inventarier, scheman, närvaro och rapporter. En React-frontend pratar med en
Spring Boot-backend via ett REST-API. Backend äger all affärslogik — frontend visar bara data och
skickar formulär, den duplicerar aldrig regler som "vem får se vad".

## 2. Säkerhetsmetoden: JWT, inte sessioner

### Varför JWT?

Ett vanligt sätt att hantera inloggning är server-side sessions: servern sparar "användare X är
inloggad" i minnet eller en databas, och skickar ett session-ID till klienten i en cookie. Vi
använder istället **JWT (JSON Web Token)**, av ett par konkreta skäl:

- **Stateless** — servern behöver inte komma ihåg något om vem som är inloggad mellan anrop. Varje
  request bär med sig ett token som redan bevisar vem användaren är och vilka roller den har.
  Det gör det enklare att skala (flera backend-instanser kan verifiera samma token utan att dela
  session-data) och enklare att testa (ett token fungerar likadant oavsett vilken server-instans
  som tar emot det).
- **Ett REST-API med en separat frontend passar naturligt med token-baserad auth** — frontend
  sparar token, skickar det som `Authorization: Bearer <token>` på varje anrop, och backend
  behöver ingen cookie-hantering eller CSRF-skydd på samma sätt som en traditionell
  server-renderad webbapp.

### Hur det fungerar i kod

1. `POST /api/auth/login` tar emot e-post + lösenord, verifierar mot BCrypt-hashat lösenord i
   databasen, och returnerar ett signerat JWT (innehåller användarens e-post och roller, med ett
   utgångsdatum).
2. Varje efterföljande request går genom en `JwtAuthenticationFilter` (körs före Spring Securitys
   vanliga inloggningsfilter). Filtret läser tokenet, verifierar signaturen, och om det är giltigt
   sätter det en autentiserad `Authentication`-kontext för den här requesten — helt utan att prata
   med databasen för att "kolla sessionen".
3. `SecurityConfig` definierar sedan, per URL-mönster och HTTP-metod, vilka roller som får komma
   åt vad — till exempel kräver `/api/attendance/**` `SUPER_ADMIN`, `ADMIN`, `DIRECTOR` eller
   `TEACHER`. Det här är deklarativt: en lista av regler, inte kod utspridd i varje controller.
4. För finkornigare regler som inte går att uttrycka som en enkel URL-regel — till exempel "en
   lärare får bara se elever i grupper hen faktiskt är tilldelad just nu" — kollas det i
   respektive service-klass, inte i `SecurityConfig`. Säkerhet finns alltså på två nivåer: en grov
   roll-nivå (vem får överhuvudtaget nå den här endpointen) och en finare data-nivå (vilken
   delmängd av datan får den här användaren se).

## 3. Hur roller hanteras

Det finns sex roller: `SUPER_ADMIN`, `ADMIN`, `DIRECTOR`, `TEACHER`, `FINANCE`, `PARENT`. En
användare kan ha **flera roller samtidigt** (en lärare kan också få `FINANCE`-rollen om hen behöver
hantera betalningar, utan att vi behöver skapa en ny "lärare-som-också-gör-ekonomi"-roll).

### Rank-systemet

Varje roll har en numerisk **rank**: `SUPER_ADMIN=100`, `ADMIN`/`DIRECTOR=90`,
`TEACHER`/`FINANCE=10`, `PARENT=0`. När någon ger eller tar bort en roll från en användare
(`POST/DELETE /api/users/{userId}/roles`), krävs det att den som utför ändringen har en egen
maxrank som är **större eller lika med** rangen på rollen som ges/tas bort. Det betyder:

- En `ADMIN` (rank 90) kan ge någon `TEACHER` eller `FINANCE` (rank 10), och kan ge en annan
  `ADMIN` eller `DIRECTOR` (samma rank, 90) — men kan aldrig ge någon `SUPER_ADMIN` (rank 100,
  högre än sin egen).
- Systemet tillåter aldrig heller att ta bort `SUPER_ADMIN`-rollen från den sista personen som har
  den — annars skulle man kunna låsa ute alla från full åtkomst av misstag.

### Varför vi valde det här istället för ett granulärt rättighetssystem

Vi diskuterade ett mer detaljerat behörighetssystem — typ "kryssa i enskilda rättigheter per
användare" istället för färdiga roller. Kunden valde de färdiga rollerna eftersom det är mycket
enklare att förstå och underhålla för ett litet team, och täckningen (sex roller, flera per
person) räcker för de faktiska behoven idag. Ett granulärt system hade varit onödig komplexitet
för storleken på verksamheten.

## 4. Problem och buggar vi stötte på (och varför de hände)

### 500 istället för 404/400 — samma rotorsak två gånger

Backend har en global felhanterare (`GlobalExceptionHandler`) med specifika handlers för kända
fel (ResourceNotFoundException -> 404, BadRequestException -> 400, och så vidare), plus en
generell "fångar allt annat"-handler som svarar 500. Problemet: två olika, helt vanliga
Spring-undantag saknade en egen specifik handler och föll därför igenom till 500-fallet, även fast
de egentligen inte var serverfel:

1. En förfrågan på en fil som inte längre fanns (en borttagen elevbild) gav 500 istället för
   404, eftersom `NoResourceFoundException` (Springs eget "hittar inte filen"-undantag) inte hade
   en egen handler.
2. Ett API-anrop med ett felaktigt format på en parameter (till exempel `materialId=undefined`
   istället för ett tal) gav 500 istället för 400, av exakt samma anledning —
   `MethodArgumentTypeMismatchException` saknade en egen handler.

Båda åtgärdades på samma sätt: lägg till en specifik handler som svarar med rätt statuskod
istället för att låta dem falla igenom till den generella 500:an. Lärdom: en "fånga allt"-handler
är bra som sista skyddsnät, men den gömmer riktiga klientfel som rätt statuskod-hantering annars
hade fångat automatiskt.

### 401 istället för 403 vid nekad behörighet

När en användare med fel roll försökte nå en skyddad endpoint fick den 401 (ej autentiserad)
istället för korrekt 403 (autentiserad men saknar behörighet). Orsaken: Springs
`response.sendError()` gör en intern vidarebefordran till `/error`-vägen, och det interna anropet
tappade säkerhetskontexten på vägen, så den slutgiltiga statuskoden blev fel. Lösning: `/error`
markerades explicit som tillåten utan autentisering, så den interna vidarebefordran inte
skrivs över av inloggningsfiltret.

### Rabatt-systemet: byggt om två gånger samma dag

Det här är det tydligaste exemplet på "bygg, testa mot verklig data, hitta problemet, fixa" under
projektet:

1. **Första versionen**: en rabatt var en regel kopplad till en elev och ett datumintervall,
   applicerad automatiskt varje månad. Fungerade i teorin.
2. **Bugg 1, hittad genom att testa live**: att skapa eller ta bort en rabatt påverkade inte den
   redan skapade fakturan för den aktuella månaden — familjen såg ingen skillnad förrän nästa
   månads faktura genererades. Orsaken var att rabattberäkningen bara kördes en gång, vid
   fakturans skapande, aldrig igen.
3. **Bugg 2, hittad direkt efter fix av bugg 1**: när vi lade till "räkna om direkt när en rabatt
   ändras" upptäckte vi att uträkningen jämförde fel datum (fakturans periodstart istället för
   dagens datum), så en rabatt skapad mitt i månaden aldrig räknades som giltig för en redan
   skapad faktura.
4. **Bugg 3, det riktiga designfelet**: efter att ha fixat både 1 och 2 testade kunden det live
   och upptäckte att en rabatt för en elev påverkade *alla* fakturatyper för den eleven samtidigt
   (både månadsavgift OCH lunch, plus gamla förfallna fakturor från andra månader) — inte bara den
   specifika fakturan man ville ge rabatt på.

Lösningen blev att **ta bort hela det regel-baserade rabattsystemet** och bygga om det enklare: en
rabatt lever nu direkt på den specifika fakturan (`StudentCharge`), inte som en separat regel som
tolkas om varje månad. Det gör det omöjligt att av misstag påverka fel faktura, eftersom det inte
längre finns någon "regel" att tolka — bara ett fältvärde på exakt en rad i databasen.

**Lärdom**: den första lösningen fungerade i tester men inte i verklig användning, eftersom
testerna aldrig provade "vad händer med flera olika fakturatyper för samma elev samtidigt".
Att testa mot verklig, levande data (inte bara automatiserade tester) avslöjade problem som annars
hade nått kunden.

## 5. Dockers roll i projektet

Docker används för att göra "det fungerar på min dator" irrelevant — hela miljön (backend +
databas) definieras i kod (`docker-compose.yml`) och startar identiskt oavsett vem som kör den
eller på vilken maskin.

### Vad `docker compose up` faktiskt startar

Två tjänster:

1. **`mysql`** — en riktig MySQL 8.4-databas i en egen container. Data sparas i en **named
   volume** (`preschool_mysql_data`), vilket betyder att databasen överlever att containern
   startas om eller byggs om — den försvinner bara om man explicit tar bort volymen
   (`docker compose down -v`).
2. **`backend`** — själva Spring Boot-applikationen, byggd från ett `Dockerfile` (Java 25). Den
   väntar på att MySQL-tjänsten är "healthy" (en riktig health check, inte bara "containern har
   startat") innan den själv startar, så den aldrig försöker koppla upp sig mot en databas som
   inte är redo än.

### Hur backend hanterar databasen vid start

När backend-containern startar kör den **Flyway**, ett verktyg som automatiskt applicerar
databas-migrationer — små SQL-filer numrerade i ordning (`V1__...`, `V2__...`, och så vidare) som
tillsammans bygger upp hela databasschemat steg för steg. Det betyder:

- En helt tom databas får exakt samma slutgiltiga struktur som en som redan körts ett tag, bara
  genom att köra alla migrationer i ordning från början.
- När vi ändrar databasschemat (till exempel lägger till en ny kolumn för sen betalningsavgift)
  skriver vi en ny migrationsfil istället för att ändra en gammal — historiken är alltid komplett
  och spårbar, och samma migrationsprocess funkar för en utvecklares lokala databas som för en
  framtida produktionsdatabas.

### Filer som laddas upp (bilder, kvitton)

Uppladdade filer (profilbilder, fotoalbum, PDF-kvitton) sparas på disk i containern, men i
**egna named volumes** separata från databasen (`preschool_uploads_data`,
`preschool_receipts_data`) — samma princip som databasen: filerna överlever att containern byggs
om, försvinner bara om volymen explicit raderas.

### Varför vi gör så här, kortfattat

- **Reproducerbarhet** — samma miljö för alla, ingen "det funkar hos mig men inte hos dig".
- **Isolering** — databasen och backend-koden kan uppdateras och byggas om oberoende av varandra.
- **Enkel lokal utveckling** — ett kommando (`docker compose up --build`) ger en komplett,
  fungerande miljö utan att någon manuellt behöver installera och konfigurera MySQL för hand.
