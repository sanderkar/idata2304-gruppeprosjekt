# SHA-protokoll 1

## 1. Introduksjon

Dette dokumentet beskriver protokollen mellom sensornoder, kontrollpaneler og en mellomserver i SHA. Kontrollpanelet kan finne tilkoblede noder, hente målinger og aktuatorstatus og styre en bestemt aktuator.

Versjon 1 støtter registrering, tilgangskontroll, avlesning og styring av simulerte enheter. Serveren kan tildele adresser automatisk til sensornoder. Trafikken er kryptert med TLS 1.3. Klientene kobler seg til på nytt ved midlertidige nettverksbrudd.

## 2. Terminologi

| Begrep | Betydning |
| --- | --- |
| Sensornode | Program som simulerer sensorer og aktuatorer og utfører kommandoer adressert til noden. |
| Kontrollpanel | Klientprogram som viser målinger og status og sender kommandoer på vegne av en bruker. |
| Mellomserver | TCP-server som registrerer forbindelser, sjekker tilgang og videresender forespørsler og svar. |
| Sensor | Komponent som leverer en måling, for eksempel temperatur eller luftfuktighet. |
| Aktuator | Komponent med en styrbar tilstand, for eksempel en varmeovn eller vifte. |
| Økt | Tiden fra en klient har opprettet en TCP-forbindelse til den lukkes. |
| Advarsel | Lokal markering i kontrollpanelet når en måling overskrider en definert grense. |

## 3. Transport

Protokollen bruker TLS 1.3 over TCP. TCP leverer en ordnet strøm av byte, slik at applikasjonen kan lese en forespørsel og tilhørende svar i rekkefølge. TLS beskytter denne strømmen under transport. Applikasjonen avgrenser meldingene med linjeskift inne i TLS-forbindelsen. Et TCP-brudd kan fortsatt gjøre utfallet av en kommando ukjent.

## 4. Portnummer

Standardporten er TCP `5000`. Lytteadressen konfigureres med
`SMART_HOME_HOST`, og porten konfigureres med `SMART_HOME_PORT`, enten i
`config/local.properties` eller som miljøvariabler. Standardverdiene er
`127.0.0.1` og `5000`. Serveren, sensornodene og kontrollpanelene bruker den
samme konfigurasjonen.

## 5. Arkitektur

Både sensornoder og kontrollpaneler starter utgående TCP-forbindelser til mellomserveren. Serveren holder oversikt over aktive forbindelser og hvilken node-ID hver nodeforbindelse tilhører. Flere kontrollpaneler og flere noder kan være registrert samtidig.

Kontrollpanelet trenger bare adressen til serveren og identifikatorene til utstyret. Serveren bruker node-ID for å finne forbindelsen. Noden bruker sensor-ID eller aktuator-ID for å finne komponenten. To forskjellige noder kan derfor ha hver sin aktuator med samme ID.

## 6. Informasjonsflyt

1. En sensornode kobler til og sender `HELLO_NODE` med en statisk adresse eller `HELLO_AUTO_NODE` for å få en adresse fra serveren. Serveren sjekker legitimasjonen og registrerer noden.
2. Et kontrollpanel kobler til og sender `HELLO_PANEL`. Serveren sjekker legitimasjonen og henter rollen fra sin konfigurasjon.
3. Kontrollpanelet sender `LIST_NODES` for å oppdage aktive noder og `LIST_DEVICES` for å se hvilke komponenter en node har.
4. Kontrollpanelet spør etter målinger og status med `GET_READING` og `GET_ACTUATOR`. Dette er en pull-modell. Kontrollpanelet kan spørre på nytt ved behov eller med jevne mellomrom.
5. Serveren sjekker tilgang før den sender en forespørsel videre. Noden utfører forespørselen og svarer. Serveren sender resultatet tilbake til kontrollpanelet som spurte.

Kontrollpanelet kan markere en avvikende temperatur ved visning, for eksempel når en måling er over 35 °C. Grensen er en lokal innstilling i kontrollpanelet. Protokollen sender selve målingen, slik at grensen kan endres uten å endre meldingsformatet.

## 7. Tilkobling og tilstand

Protokollen er tilkoblingsorientert og har tilstand. Etter et vellykket TLS-håndtrykk må første applikasjonsmelding være en `HELLO`-melding. Ved `READY;1` eller `READY_NODE;1;<nodeId>` er forbindelsen registrert og kan gjenbrukes. En registrert klient kan ikke skifte identitet på samme forbindelse.

Et kontrollpanel venter på svar før det sender neste forespørsel. Serveren har høyst én utestående forespørsel per sensornode, inkludert forbindelsessjekker. Kommer det en ny forespørsel til en opptatt node, svarer serveren med `ERROR;BUSY`. Dermed kan svaret kobles til riktig forespørsel uten egne forespørsels-ID-er. En avbrutt nodeforbindelse fjernes fra registeret.

## 8. Typer og konstanter

| Felt | Format og betydning |
| --- | --- |
| `version` | Heltall. Denne versjonen bruker `1`. |
| `nodeId`, `sensorId`, `actuatorId`, `heaterId` | Heltall fra 0 til 2147483647, skrevet med ASCII-sifre. Node-ID er unik blant registrerte noder. Sensor-ID er unik blant sensorer på noden, og aktuator-ID er unik blant aktuatorer på noden. `heaterId` er aktuator-ID-en til en varmeovn. |
| `deviceName` | Navn for automatisk tildeling. Det har 1–32 tegn, begynner med en stor ASCII-bokstav og kan deretter inneholde store ASCII-bokstaver, sifre og understrek. |
| `userId`, `token` | Ikke-tomme strenger på opptil 128 tegn fra `A-Z`, `a-z`, `0-9`, punktum, bindestrek og understrek. Token behandles som en hemmelig verdi. |
| `sensorType` | `TEMPERATURE` for grader Celsius og `HUMIDITY` for relativ luftfuktighet i prosent fra 0 til 100. |
| `actuatorType` | `HEATER` for varmeovn og `FAN` for vifte. |
| `value` | Endelig desimaltall med ASCII-sifre, eventuelt minustegn og punktum som desimalskilletegn. Eksempel `21.5`. |
| `state` | `ON` eller `OFF`. |

Nye typer kan legges til ved å avtale en ny typekode og tilhørende enhet eller tilstand. Nodene simulerer temperatur, luftfuktighet, varmeovn og vifte.

## 9. Meldingsformat

Hver melding er én UTF-8-linje på maksimalt 4096 byte før linjeskiftet. Avsender avslutter linjen med LF (`\\n`). Mottakeren kan også akseptere CRLF (`\\r\\n`). Meldingsnavn skrives med store bokstaver. Felt skilles med semikolon. Mellomrom rundt felt, tomme felt og ekstra felt avvises. Feltene har faste posisjoner. Notasjonen `<felt>` nedenfor betyr at avsender setter inn en verdi uten vinkelparentesene.

| Melding | Avsender → mottaker | Svar ved suksess |
| --- | --- | --- |
| `HELLO_NODE;<version>;<nodeId>;<token>` | Node → server | `READY;1` |
| `HELLO_NODE;<version>;<nodeId>;<token>;HEARTBEAT` | Node → server | `READY;1` |
| `HELLO_AUTO_NODE;<version>;<deviceName>;<token>` | Node → server | `READY_NODE;1;<nodeId>` |
| `HELLO_PANEL;<version>;<userId>;<token>` | Kontrollpanel → server | `READY;1` |
| `PING` | Kontrollpanel → server eller server → automatisk node eller statisk node med `HEARTBEAT` | `PONG` tilbake til avsenderen |
| `LIST_NODES` | Kontrollpanel → server | `NODES` etterfulgt av ett semikolonseparert felt per aktiv node-ID |
| `LIST_DEVICES;<nodeId>` | Kontrollpanel → server → node | `DEVICES;<nodeId>` etterfulgt av ett felt per komponent |
| `GET_READING;<nodeId>;<sensorId>` | Kontrollpanel → server → node | `READING;<nodeId>;<sensorId>;<sensorType>;<value>` |
| `GET_ACTUATOR;<nodeId>;<actuatorId>` | Kontrollpanel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` |
| `SET_ACTUATOR;<nodeId>;<actuatorId>;<state>` | Kontrollpanel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` med faktisk tilstand etter utførelse |
| `TURN_ON_HEATER;<nodeId>;<heaterId>` | Kontrollpanel → server → node | Samme svar som `SET_ACTUATOR` med tilstand `ON` og type `HEATER` |
| `TURN_OFF_HEATER;<nodeId>;<heaterId>` | Kontrollpanel → server → node | Samme svar som `SET_ACTUATOR` med tilstand `OFF` og type `HEATER` |

De to varmeovnkommandoene beholdes for å støtte tidligere kontrollpaneler. Noden avviser kommandoen med `UNKNOWN_ACTUATOR` hvis ID-en ikke tilhører en varmeovn. For videresendte forespørsler går svaret fra node via server til kontrollpanelet.

Eksempler på listesvar er `NODES;7;12` og `DEVICES;12;SENSOR:1:TEMPERATURE;ACTUATOR:3:HEATER;ACTUATOR:4:FAN`. Hvert komponentfelt har formen `kategori:id:type`. Et tomt resultat inneholder bare `NODES` eller `DEVICES;<nodeId>`, uten semikolon på slutten.

## 10. Feilhåndtering

Feil sendes som `ERROR;<code>`. Kontrollpanelet viser en forklaring til brukeren. Serveren videresender bare gyldige svar som samsvarer med den ventende forespørselen.

| Kode | Når den brukes |
| --- | --- |
| `BAD_FORMAT` | Feil antall felt, tomt obligatorisk felt, ugyldig verdi eller ID utenfor gyldig område. |
| `UNKNOWN_COMMAND` | Meldingsnavnet er ukjent. |
| `BAD_STATE` | Meldingen er kjent, men kan ikke brukes i gjeldende tilstand. |
| `UNSUPPORTED_VERSION` | Registreringsmeldingen ber om en annen protokollversjon. |
| `AUTH_FAILED` | Legitimasjonen er ugyldig. |
| `FORBIDDEN` | Brukerens rolle mangler tilgang til handlingen. |
| `DUPLICATE_NODE` | En aktiv node er allerede registrert med ID-en. |
| `ADDRESS_IN_USE` | En statisk node prøver å bruke en adresse som allerede er tildelt en automatisk node. |
| `ADDRESS_UNAVAILABLE` | Serveren kunne ikke tildele og lagre en ny adresse. |
| `NODE_UNAVAILABLE` | Ingen aktiv forbindelse finnes for node-ID-en, eller den forsvinner mens et svar ventes. |
| `WRONG_NODE` | En sensornode mottar en forespørsel adressert til en annen node. |
| `UNKNOWN_SENSOR` | Sensor-ID-en finnes ikke på noden. |
| `UNKNOWN_ACTUATOR` | Aktuator-ID-en finnes ikke eller matcher ikke den etterspurte typen. |
| `BUSY` | Noden behandler allerede en forespørsel. |
| `TIMEOUT` | Noden svarer ikke innen fristen. |
| `BAD_RESPONSE` | Noden sender et svar som er ugyldig eller ikke matcher forespørselen. |

Feil under registrering avslutter forbindelsen etter feilsvaret. En ugyldig forespørsel etter registrering avvises uten å utføre handlingen. Ved brudd midt i en melding forkastes den ufullstendige linjen. En linje som overskrider størrelsesgrensen fører til at mottakeren lukker forbindelsen. Etter tidsavbrudd eller ugyldig svar fra en node lukker serveren nodeforbindelsen, slik at et sent eller feil svar ikke kan forveksles med svaret på neste forespørsel.

## 11. Eksempelscenario

Node 12 har temperatursensor 1 og varmeovn 3. Nodens og eierens legitimasjon er satt opp på serveren på forhånd. Tokenene nedenfor er eksempelverdier.

| Fra → til | Melding |
| --- | --- |
| Node → server | `HELLO_NODE;1;12;node-example;HEARTBEAT` |
| Server → node | `READY;1` |
| Kontrollpanel → server | `HELLO_PANEL;1;owner-example;panel-example` |
| Server → kontrollpanel | `READY;1` |
| Kontrollpanel → server | `LIST_NODES` |
| Server → kontrollpanel | `NODES;12` |
| Kontrollpanel → server → node | `LIST_DEVICES;12` |
| Node → server → kontrollpanel | `DEVICES;12;SENSOR:1:TEMPERATURE;ACTUATOR:3:HEATER` |
| Kontrollpanel → server → node | `GET_READING;12;1` |
| Node → server → kontrollpanel | `READING;12;1;TEMPERATURE;18.0` |
| Kontrollpanel → server → node | `TURN_ON_HEATER;12;3` |
| Node → server → kontrollpanel | `ACTUATOR_STATE;12;3;HEATER;ON` |

Brukeren ser 18 °C og slår på ovnen. Kontrollpanelet viser ovnen som på først etter at svarmeldingen er mottatt.

### Automatisk adressetildeling

1. En node startes med `--auto KITCHEN` og sender `HELLO_AUTO_NODE;1;KITCHEN;kitchen-example` over TLS.
2. Serveren sjekker tokenet som er konfigurert for `KITCHEN`. Første gang velger den en ledig adresse i intervallet 100000–999999999 og lagrer tildelingen før den svarer. Serveren hopper over alle konfigurerte statiske adresser, også for noder som er frakoblet.
3. Serveren svarer for eksempel `READY_NODE;1;100000`. Noden bruker adressen i de vanlige protokollmeldingene, og panelet kan finne den med `LIST_NODES`. Automatiske noder svarer på forbindelsessjekker.
4. Ved gjenoppkobling og etter omstart finner serveren samme adresse ut fra nodenavnet. En ekstra forbindelse for en node som allerede er aktiv får `ERROR;DUPLICATE_NODE`. Tildelinger beholdes når en node kobler fra, slik at adressebaserte tilgangsregler og panelkommandoer fortsetter å peke på samme node.

Dette lar serveren sikre unike, stabile adresser selv når flere noder starter samtidig. Hver node trenger bare et klargjort navn og token for å be om en adresse.

## 12. Pålitelighet

- **Oppdage brudd.** Kontrollpanelet sjekker forbindelsen med `PING` i bakgrunnen. Det venter to sekunder mellom sjekkene og inntil ti sekunder på svar. Serveren prøver å sende `PING` til en node med `HEARTBEAT` etter tre sekunder og hopper over sjekken hvis noden allerede har en forespørsel. Serveren venter inntil fem sekunder på svar fra noden, og noden venter inntil femten sekunder på neste melding. Forbindelsessjekkene gjør at også et brudd uten TCP-avslutning kan oppdages når ingen bruker systemet. Eldre noder kan fortsatt bruke registreringsformatet uten `HEARTBEAT`.
- **Koble til på nytt.** Begge klientene oppretter en ny TCP-forbindelse og sender en ny `HELLO`. Noden øker ventetiden mellom mislykkede forsøk fra ett til to og deretter fire sekunder. Den prøver også igjen ved `DUPLICATE_NODE`, slik at serveren kan få tid til å fjerne den gamle forbindelsen. Ved avvist legitimasjon kreves korrigert konfigurasjon og omstart. Simulerte målinger og aktuatorstatus blir i nodeprosessen gjennom gjenoppkoblingen.
- **Gjenta avlesninger.** Kontrollpanelet holder én leseforespørsel om gangen og kan sende den på nytt i et vindu på femten sekunder ved tapt forbindelse, utilgjengelig node, opptatt node eller tidsavbrudd. Et påbegynt forsøk får fullføre innenfor sin svarfrist. Panelet venter et halvt sekund mellom forsøk. En ny sensoravlesning kan gi en annen verdi.
- **Vise bufrede målinger.** Kontrollpanelet beholder de siste bekreftede verdiene for opptil 100 sensorer i minnet. Hvis en sensor fortsatt er utilgjengelig etter nye forsøk, kan panelet vise en bufret verdi med tidspunktet den ble mottatt. Målinger genereres ved forespørsel. Verdier fra tiden uten forbindelse blir derfor ikke etterfylt.
- **Vise usikkert utfall for styring.** Panelet sender bare en styringskommando én gang. Hvis svaret går tapt, kan kommandoen allerede ha endret tilstanden. Panelet ber brukeren hente aktuell status etter gjenoppkobling, siden automatisk gjentakelse kan overstyre en nyere kommando fra et annet panel.

## 13. Sikkerhet

Serveren har en lokal konfigurasjon som knytter hver tillatt statiske node-ID og hvert tillatte automatiske nodenavn til et token. Konfigurasjonen knytter også hver bruker-ID til et token og en rolle. Tilgang avgjøres på serveren før videresending. En nodeforbindelse kan svare på forespørsler som er sendt til den, men har ikke tilgang til kontrollpanelkommandoer.

| Rolle | Tillatt tilgang |
| --- | --- |
| Eier | Se noder, komponenter, målinger og status, og endre aktuatorenes tilstand. |
| Tekniker | Se noder, komponenter, målinger og status for feilsøking. |
| Gjest | Se noder, komponenter og sensormålinger. |

TLS 1.3 gir kryptering, integritetsbeskyttelse og autentisering av mellomserveren. Både sensornoder og kontrollpaneler har en lokal kopi av serverens offentlige X.509-sertifikat. Klienten kontrollerer tillit til sertifikatet og at servernavnet eller IP-adressen stemmer. En mislykket TLS-forbindelse åpner ingen applikasjonsøkt. Hver ny forbindelse går gjennom TLS før klienten sender tokenet i `HELLO`.

Serverens private nøkkel ligger i et passordbeskyttet PKCS#12-nøkkellager. Klientene autentiserer seg med de eksisterende tokenene inne i TLS-forbindelsen. TLS beskytter begge forbindelsene, og serveren leser meldingene for å kontrollere tilgang og rute dem. Vi bruker Java sin TLS-implementasjon slik at applikasjonen kan beholde meldingsformatet sitt og bruke standardisert transportkryptering. Oppsettet er beskrevet i [kjøreveiledningen](docs/running.md). Token, passord og private nøkler holdes utenfor Git og logger.

## 14. Antakelser

- Sensorer og aktuatorer er simulert. Noder har et lokalt komponentoppsett og får adressen fra programargumentet eller serveren.
- Mellomserveren er eneste kontaktpunkt. En server er tilstrekkelig for demonstrasjonen og gir ett sted å registrere noder og håndheve tilgang.
- Linjebaserte meldinger med faste felt er valgt for at de skal være enkle å lese og feilsøke.
- Meldinger og aktive forbindelser lagres i minnet. Serveren lagrer automatiske adressetildelinger lokalt, slik at klientene kan få samme adresse når de registrerer seg etter en omstart.
- Kontrollpanelet spør etter gjeldende verdier. Historiske målinger, abonnement og automatisk styring inngår ikke i versjon 1.
