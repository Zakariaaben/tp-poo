package transport.services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import transport.core.CartePersonnelle;
import transport.core.ModeDePaiement;
import transport.core.Personne;
import transport.core.ReductionImpossibleException;
import transport.core.Ticket;
import transport.core.TitreTransport;
import transport.core.TypeCarte;

/**
 * Service class to manage TitreTransport entities Handles JSON
 * serialization/deserialization and CRUD operations
 */
public class TitreTransportService {

    private static final Logger LOGGER = Logger.getLogger(TitreTransportService.class.getName());
    private static final String DATA_DIRECTORY = "data";
    private static final String TITRE_FILE = DATA_DIRECTORY + "/titres.json";
    private List<TitreTransport> titres = new ArrayList<>();
    private final Gson gson;
    private static  PersonneService personneService;

    public TitreTransportService(PersonneService personneService) {
        this.personneService = personneService;

        // Create custom GSON instance with type adapters
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeAdapter(TitreTransport.class, new TitreTransportAdapter())
                .registerTypeAdapter(new TypeToken<ArrayList<TitreTransport>>() {
                }.getType(), new TitreTransportListAdapter())
                .setPrettyPrinting()
                .create();

        // Ensure data directory exists
        File directory = new File(DATA_DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            LOGGER.severe("Failed to create data directory: " + DATA_DIRECTORY);
        }

        // Load existing data
        loadData();
    }

    /**
     * Retrieves all TitreTransport entities
     */
    public List<TitreTransport> getAllTitres() {
        return new ArrayList<>(titres);
    }

    /**
     * Retrieves TitreTransport entities for a specific person
     */
    public List<TitreTransport> getTitresForPerson(UUID personneId) {
        return titres.stream()
                .filter(t -> t.getPersonneId().equals(personneId))
                .collect(Collectors.toList());
    }

    /**
     * Creates a new Ticket
     */
    public Ticket createTicket(Personne personne, ModeDePaiement modeDePaiement) {
        Ticket ticket = new Ticket(personne, LocalDateTime.now());
        titres.add(ticket);
        saveData();
        return ticket;
    }

    /**
     * Creates a new CartePersonnelle
     */
    public CartePersonnelle createCarte(Personne personne, ModeDePaiement modeDePaiement) throws ReductionImpossibleException {
        CartePersonnelle carte = new CartePersonnelle(personne);
        titres.add(carte);
        saveData();
        return carte;
    }

    /**
     * Use a TitreTransport
     */
    public boolean useTicket(TitreTransport titre) {
        if (titre instanceof Ticket) {
            Ticket ticket = (Ticket) titre;
            try {
                ticket.useTicket();
                saveData();
                return true;
            } catch (IllegalStateException e) {
                LOGGER.warning("Cannot use invalid ticket: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Save a TitreTransport
     */
    public void saveTitre(TitreTransport titre) {
        // Check if ticket already exists
        boolean exists = false;
        for (int i = 0; i < titres.size(); i++) {
            if (titres.get(i).getCurrentId().equals(titre.getCurrentId())) {
                titres.set(i, titre);
                exists = true;
                break;
            }
        }

        if (!exists) {
            titres.add(titre);
        }

        saveData();
    }

    /**
     * Delete a TitreTransport
     */
    public boolean deleteTitre(Integer id) {
        boolean removed = titres.removeIf(t -> t.getCurrentId().equals(id));
        if (removed) {
            saveData();
        }
        return removed;
    }

    /**
     * Loads TitreTransport data from the JSON file
     */
    private void loadData() {
        File file = new File(TITRE_FILE);

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type titresListType = new TypeToken<ArrayList<TitreTransport>>() {
                }.getType();
                List<TitreTransport> loadedTitres = gson.fromJson(reader, titresListType);

                if (loadedTitres != null) {
                    titres = loadedTitres;
                } else {
                    titres = new ArrayList<>();
                }

                LOGGER.info("Loaded " + titres.size() + " TitreTransport records");
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.log(Level.SEVERE, "Error loading TitreTransport data", e);
                titres = new ArrayList<>();
            }
        } else {
            LOGGER.info("No existing TitreTransport data file found. Starting with empty list.");
        }
    }

    /**
     * Saves TitreTransport data to the JSON file
     */
    private void saveData() {
        try (Writer writer = new FileWriter(TITRE_FILE)) {
            Type titresListType = new TypeToken<ArrayList<TitreTransport>>() {
            }.getType();
            String jsonData = gson.toJson(titres, titresListType);
            writer.write(jsonData);
            LOGGER.info("Successfully saved " + titres.size() + " TitreTransport records");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving TitreTransport data", e);
        }
    }

    /**
     * Type adapter for LocalDate
     */
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public JsonElement serialize(LocalDate localDate, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(localDate));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return LocalDate.parse(json.getAsString(), formatter);
            } catch (Exception e) {
                throw new JsonParseException("Error parsing LocalDate: " + json.getAsString(), e);
            }
        }
    }

    /**
     * Type adapter for LocalDateTime
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(localDateTime));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return LocalDateTime.parse(json.getAsString(), formatter);
            } catch (Exception e) {
                throw new JsonParseException("Error parsing LocalDateTime: " + json.getAsString(), e);
            }
        }
    }

    /**
     * Type adapter for UUID
     */
    private static class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {

        @Override
        public JsonElement serialize(UUID uuid, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(uuid.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return UUID.fromString(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Error parsing UUID: " + json.getAsString(), e);
            }
        }
    }

    /**
     * Type adapter for TitreTransport and subclasses
     */
    private static class TitreTransportAdapter implements JsonSerializer<TitreTransport>, JsonDeserializer<TitreTransport> {

        @Override
        public JsonElement serialize(TitreTransport titre, Type type, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            // Determine the type
            String className;
            if (titre instanceof Ticket) {
                className = "Ticket";
            } else if (titre instanceof CartePersonnelle) {
                className = "CartePersonnelle";
            } else {
                throw new JsonParseException("Unknown TitreTransport type: " + titre.getClass().getName());
            }

            // Add type at the root level
            result.add("type", new JsonPrimitive(className));

            // Create data object to hold all fields
            JsonObject dataObject = new JsonObject();

            // Add common fields to data object
            dataObject.add("currentId", new JsonPrimitive(titre.getCurrentId()));
            dataObject.add("dateAchat", context.serialize(titre.getDateAchat()));
            dataObject.add("prix", new JsonPrimitive(Integer.parseInt(titre.getPrix())));
            dataObject.add("personneId", context.serialize(titre.getPersonneId()));

            // Add specific fields directly to data object based on type
            if (titre instanceof Ticket) {
                Ticket ticket = (Ticket) titre;
                dataObject.add("used", new JsonPrimitive(ticket.isUsed()));
            } else if (titre instanceof CartePersonnelle) {
                CartePersonnelle carte = (CartePersonnelle) titre;
                dataObject.add("type", context.serialize(carte.getType()));
            }

            // Add the data object to the result
            result.add("data", dataObject);
            return result;
        }

        @Override
        public TitreTransport deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();


                if (!jsonObject.has("type") || !jsonObject.has("data")) {
                    throw new JsonParseException("Invalid TitreTransport JSON structure: missing type or data");
                }

                String type = jsonObject.get("type").getAsString();
                JsonObject data = jsonObject.get("data").getAsJsonObject();

                // Extract common fields from data object
                Integer currentId = data.get("currentId").getAsInt();
                LocalDateTime dateAchat = context.deserialize(data.get("dateAchat"), LocalDateTime.class);
                Integer prix = data.has("prix")
                        ? (data.get("prix").isJsonPrimitive() ? data.get("prix").getAsInt() : Integer.parseInt(data.get("prix").getAsString())) : 0;
                UUID personneId = context.deserialize(data.get("personneId"), UUID.class);

                TitreTransport titre;

                Personne personne = personneService.getPersonneById(personneId);
                switch (type) {
                    case "Ticket":
                        boolean used = data.has("used") && data.get("used").getAsBoolean();

                        // get personne with id personneId
                        if (personne == null) {
                            throw new JsonParseException("Personne not found for ID: " + personneId);
                        }

                        titre = new Ticket(personne, dateAchat);
                        ((Ticket) titre).setUsed(used);
                        break;

                    case "CartePersonnelle":
                        TypeCarte typeCarte = data.has("type")
                                ? context.deserialize(data.get("type"), TypeCarte.class) : null;



                        titre = new CartePersonnelle(personne, dateAchat, prix, typeCarte);
                        break;

                    default:
                        throw new JsonParseException("Unknown TitreTransport type: " + type);
                }

                // Set common fields using reflection to bypass constructor
                setFieldValue(titre, "currentId", currentId);
                setFieldValue(titre, "prix", prix);
                setFieldValue(titre, "personneId", personneId);

                return titre;
            } catch (Exception e) {
                throw new JsonParseException("Error deserializing TitreTransport: " + e.getMessage(), e);
            }
        }

        private void setFieldValue(TitreTransport titre, String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = TitreTransport.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(titre, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.warning("Failed to set field " + fieldName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Type adapter for list of TitreTransport
     */
    private static class TitreTransportListAdapter implements JsonSerializer<List<TitreTransport>>,
            JsonDeserializer<List<TitreTransport>> {

        @Override
        public JsonElement serialize(List<TitreTransport> titres, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray jsonArray = new JsonArray();

            for (TitreTransport titre : titres) {
                // Use the TitreTransportAdapter for each item
                jsonArray.add(context.serialize(titre, TitreTransport.class));
            }

            return jsonArray;
        }

        @Override
        public List<TitreTransport> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            List<TitreTransport> titres = new ArrayList<>();
            JsonArray jsonArray = json.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                try {
                    TitreTransport titre = context.deserialize(element, TitreTransport.class);
                    titres.add(titre);
                } catch (Exception e) {
                    LOGGER.warning("Error deserializing TitreTransport: " + e.getMessage());
                }
            }

            return titres;
        }
    }
}
