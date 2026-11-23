# SHA-protokoll 1

## 1. Introduksjon

Dette dokumentet beskriver protokollen mellom sensornoder, kontrollpaneler og en mellomserver i SHA. Kontrollpanelet kan finne tilkoblede noder, hente målinger og aktuatorstatus og styre en bestemt aktuator.

**Status:** Protokollversjon 1 er et designforslag. Dagens kode kan simulere temperatur og varmeovner, kode og dekode `TURN_ON_HEATER` og utføre kommandoen lokalt på riktig node. Serveren har et utkast til mottak av én TCP-linje. Registrering, ruting, svarmeldinger, roller og øvrige meldingstyper nedenfor må implementeres.

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

Protokollen bruker TCP. TCP leverer en ordnet strøm av byte, slik at applikasjonen kan lese en forespørsel og tilhørende svar i rekkefølge. Applikasjonen avgrenser selv meldingene med linjeskift. Et TCP-brudd kan fortsatt gjøre utfallet av en kommando ukjent.

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

1. En sensornode kobler til og sender `HELLO_NODE`. Serveren sjekker legitimasjonen og registrerer noden.
2. Et kontrollpanel kobler til og sender `HELLO_PANEL`. Serveren sjekker legitimasjonen og henter rollen fra sin konfigurasjon.
3. Kontrollpanelet sender `LIST_NODES` for å oppdage aktive noder og `LIST_DEVICES` for å se hvilke komponenter en node har.
4. Kontrollpanelet spør etter målinger og status med `GET_READING` og `GET_ACTUATOR`. Dette er en pull-modell. Kontrollpanelet kan spørre på nytt ved behov eller med jevne mellomrom.
5. Serveren sjekker tilgang før den sender en forespørsel videre. Noden utfører forespørselen og svarer. Serveren sender resultatet tilbake til kontrollpanelet som spurte.

Kontrollpanelet kan markere en avvikende temperatur ved visning, for eksempel når en måling er over 35 °C. Grensen er en lokal innstilling i kontrollpanelet. Protokollen sender selve målingen, slik at grensen kan endres uten å endre meldingsformatet.

## 7. Tilkobling og tilstand

Protokollen er tilkoblingsorientert og har tilstand. Første melding på en ny forbindelse må være en av de to `HELLO`-meldingene. Ved `READY;1` blir forbindelsen registrert og kan gjenbrukes. En registrert klient kan ikke skifte identitet på samme forbindelse.

Et kontrollpanel venter på svar før det sender neste forespørsel. Serveren har høyst én utestående forespørsel per sensornode. Kommer det en ny forespørsel til samme opptatte node fra et annet panel, svarer serveren med `ERROR;BUSY`. Dermed kan svaret kobles til riktig kontrollpanel uten egne forespørsels-ID-er. En avbrutt nodeforbindelse fjernes fra registeret.

## 8. Typer og konstanter

| Felt | Format og betydning |
| --- | --- |
| `version` | Heltall. Denne versjonen bruker `1`. |
| `nodeId`, `sensorId`, `actuatorId`, `heaterId` | Heltall fra 0 til 2147483647, skrevet med ASCII-sifre. Node-ID er unik blant registrerte noder. Sensor-ID er unik blant sensorer på noden, og aktuator-ID er unik blant aktuatorer på noden. `heaterId` er aktuator-ID-en til en varmeovn. |
| `userId`, `token` | Ikke-tomme strenger på opptil 128 tegn fra `A-Z`, `a-z`, `0-9`, punktum, bindestrek og understrek. Token behandles som en hemmelig verdi. |
| `sensorType` | `TEMPERATURE` for grader Celsius og `HUMIDITY` for relativ luftfuktighet i prosent fra 0 til 100. |
| `actuatorType` | `HEATER` for varmeovn og `FAN` for vifte. |
| `value` | Endelig desimaltall med ASCII-sifre, eventuelt minustegn og punktum som desimalskilletegn. Eksempel `21.5`. |
| `state` | `ON` eller `OFF`. |

Nye typer kan legges til ved å avtale en ny typekode og tilhørende enhet eller tilstand. Temperatur og varmeovn finnes i prototypen. Luftfuktighet og vifte er planlagte typer.

## 9. Meldingsformat

Hver melding er én UTF-8-linje på maksimalt 4096 byte før linjeskiftet. Avsender avslutter linjen med LF (`\\n`). Mottakeren kan også akseptere CRLF (`\\r\\n`). Meldingsnavn skrives med store bokstaver. Felt skilles med semikolon. Mellomrom rundt felt, tomme felt og ekstra felt avvises. Feltene har faste posisjoner. Notasjonen `<felt>` nedenfor betyr at avsender setter inn en verdi uten vinkelparentesene.

| Melding | Avsender → mottaker | Svar ved suksess |
| --- | --- | --- |
| `HELLO_NODE;<version>;<nodeId>;<token>` | Node → server | `READY;1` |
| `HELLO_PANEL;<version>;<userId>;<token>` | Kontrollpanel → server | `READY;1` |
| `LIST_NODES` | Kontrollpanel → server | `NODES` etterfulgt av ett semikolonseparert felt per aktiv node-ID |
| `LIST_DEVICES;<nodeId>` | Kontrollpanel → server → node | `DEVICES;<nodeId>` etterfulgt av ett felt per komponent |
| `GET_READING;<nodeId>;<sensorId>` | Kontrollpanel → server → node | `READING;<nodeId>;<sensorId>;<sensorType>;<value>` |
| `GET_ACTUATOR;<nodeId>;<actuatorId>` | Kontrollpanel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` |
| `SET_ACTUATOR;<nodeId>;<actuatorId>;<state>` | Kontrollpanel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` med faktisk tilstand etter utførelse |
| `TURN_ON_HEATER;<nodeId>;<heaterId>` | Kontrollpanel → server → node | Samme svar som `SET_ACTUATOR` med tilstand `ON` og type `HEATER` |

`TURN_ON_HEATER` beholdes for å støtte meldingen som allerede finnes i prototypen. Noden avviser kommandoen med `UNKNOWN_ACTUATOR` hvis ID-en ikke tilhører en varmeovn. For videresendte forespørsler går svaret fra node via server til kontrollpanelet.

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
| `NODE_UNAVAILABLE` | Ingen aktiv forbindelse finnes for node-ID-en, eller den forsvinner mens et svar ventes. |
| `WRONG_NODE` | En sensornode mottar en forespørsel adressert til en annen node. |
| `UNKNOWN_SENSOR` | Sensor-ID-en finnes ikke på noden. |
| `UNKNOWN_ACTUATOR` | Aktuator-ID-en finnes ikke eller matcher ikke den etterspurte typen. |
| `BUSY` | Noden behandler allerede en forespørsel. |
| `TIMEOUT` | Noden svarer ikke innen fristen. |
| `BAD_RESPONSE` | Noden sender et svar som er ugyldig eller ikke matcher forespørselen. |

Feil under registrering avslutter forbindelsen etter feilsvaret. En ugyldig forespørsel etter registrering avvises uten å utføre handlingen. En linje som overskrider størrelsesgrensen fører til at mottakeren lukker forbindelsen. Etter tidsavbrudd eller ugyldig svar fra en node lukker serveren nodeforbindelsen, slik at et sent eller feil svar ikke kan forveksles med svaret på neste forespørsel.

## 11. Eksempelscenario

Node 12 har temperatursensor 1 og varmeovn 3. Nodens og eierens legitimasjon er satt opp på serveren på forhånd. Tokenene nedenfor er eksempelverdier.

| Fra → til | Melding |
| --- | --- |
| Node → server | `HELLO_NODE;1;12;node-example` |
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
| Node → server → kontrollpanel | `ACTUATOR;12;3;HEATER;ON` |

Brukeren ser 18 °C og slår på ovnen. Kontrollpanelet viser ovnen som på først etter at svarmeldingen er mottatt.

## 12. Pålitelighet

Serveren venter inntil 5 sekunder på svar fra noden etter videresending. Kontrollpanelet kan bruke en lengre svarfrist, for eksempel 10 sekunder, slik at serveren får anledning til å melde fra om tidsavbrudd. Ved tilkoblingsbrudd kan klienten koble til på nytt og registrere seg igjen.

En kommando kan ha blitt utført selv om svaret gikk tapt. Etter gjenoppkobling bør kontrollpanelet hente status før brukeren eventuelt sender kommandoen på nytt. Av/på-kommandoer angir ønsket sluttstatus, slik at et gjentatt kall med samme verdi gir samme sluttstatus. Versjon 1 garanterer ikke at en kommando utføres nøyaktig én gang.

## 13. Sikkerhet

Serveren har en lokal konfigurasjon som knytter hver tillatt node-ID til et token og hver bruker-ID til et token og en rolle. Tilgang avgjøres på serveren før videresending. En nodeforbindelse kan svare på forespørsler som er sendt til den, men har ikke tilgang til kontrollpanelkommandoer.

| Rolle | Tillatt tilgang |
| --- | --- |
| Eier | Se noder, komponenter, målinger og status, og endre aktuatorenes tilstand. |
| Tekniker | Se noder, komponenter, målinger og status for feilsøking. |
| Gjest | Se noder, komponenter og sensormålinger. |

Token skal holdes utenfor Git og utelates fra logger. TCP-trafikken i dette designet er ukryptert. En som kan lese trafikken, kan se token og meldinger. Første versjon er derfor avgrenset til lokal utvikling og et kontrollert demonstrasjonsnett. Ved bruk utenfor dette miljøet må transporten beskyttes, for eksempel med TLS.

## 14. Antakelser

- Sensorer og aktuatorer er simulert. Noder får ID og komponentoppsett fra lokal konfigurasjon.
- Mellomserveren er eneste kontaktpunkt. En server er tilstrekkelig for demonstrasjonen og gir ett sted å registrere noder og håndheve tilgang.
- Linjebaserte meldinger med faste felt er valgt for at de skal være enkle å lese og feilsøke.
- Meldinger og registreringer lagres ikke permanent på serveren. Etter omstart registrerer klientene seg igjen.
- Kontrollpanelet spør etter gjeldende verdier. Historiske målinger, abonnement og automatisk styring inngår ikke i versjon 1.
