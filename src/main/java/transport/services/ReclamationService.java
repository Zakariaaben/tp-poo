package transport.services;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import transport.core.*;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service class to manage Reclamation entities Handles JSON
 * serialization/deserialization and CRUD operations
 */
public class ReclamationService {

    private static final Logger LOGGER = Logger.getLogger(ReclamationService.class.getName());
    private static final String DATA_DIRECTORY = "data";
    private static final String RECLAMATION_FILE = DATA_DIRECTORY + "/reclamations.json";
    private List<Reclamation> reclamations = new ArrayList<>();
    private final Gson gson;
    private final PersonneService personneService;

    public ReclamationService(PersonneService personneService) {
        this.personneService = personneService;

        // Create custom GSON instance with type adapters
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeAdapter(ReclamationStatus.class, new ReclamationStatusAdapter())
                .registerTypeAdapter(ReclamationType.class, new ReclamationTypeAdapter())
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
     * Retrieves all Reclamation entities, sorted by date (most recent first)
     */
    public List<Reclamation> getAllReclamations() {
        return reclamations.stream()
                .sorted(Comparator.comparing(Reclamation::getDateReclamation).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific Reclamation by ID
     */
    public Reclamation getReclamationById(UUID id) {
        return reclamations.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves Reclamations for a specific person
     */
    public List<Reclamation> getReclamationsForPerson(UUID personneId) {
        return reclamations.stream()
                .filter(r -> r.getPersonneId().equals(personneId))
                .sorted(Comparator.comparing(Reclamation::getDateReclamation).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Creates a new Reclamation
     */
    public Reclamation createReclamation(Personne personne, String description, ReclamationType type) {
        Reclamation reclamation = new Reclamation(personne, description, type);
        reclamations.add(reclamation);
        saveData();
        return reclamation;
    }

    /**
     * Process a reclamation (treat, refuse, or cancel)
     */
    public void processReclamation(Reclamation reclamation, ReclamationStatus newStatus, String response) {
        Reclamation existing = getReclamationById(reclamation.getId());
        if (existing != null) {
            switch (newStatus) {
                case TRAITE:
                    existing.traiter(response);
                    break;
                case REFUSE:
                    existing.refuser(response);
                    break;
                case ANNULE:
                    existing.annuler();
                    break;
                default:
                    // Do nothing for other statuses
                    break;
            }
            saveData();
        }
    }

    /**
     * Save a Reclamation
     */
    public void saveReclamation(Reclamation reclamation) {
        // Check if reclamation already exists
        boolean exists = false;
        for (int i = 0; i < reclamations.size(); i++) {
            if (reclamations.get(i).getId().equals(reclamation.getId())) {
                reclamations.set(i, reclamation);
                exists = true;
                break;
            }
        }

        if (!exists) {
            reclamations.add(reclamation);
        }

        saveData();
    }

    /**
     * Delete a Reclamation
     */
    public boolean deleteReclamation(UUID id) {
        boolean removed = reclamations.removeIf(r -> r.getId().equals(id));
        if (removed) {
            saveData();
        }
        return removed;
    }

    /**
     * Loads Reclamation data from the JSON file
     */
    private void loadData() {
        File file = new File(RECLAMATION_FILE);

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type reclamationListType = new TypeToken<ArrayList<Reclamation>>() {
                }.getType();
                List<Reclamation> loadedReclamations = gson.fromJson(reader, reclamationListType);

                if (loadedReclamations != null) {
                    reclamations = loadedReclamations;
                } else {
                    reclamations = new ArrayList<>();
                }

                LOGGER.info("Loaded " + reclamations.size() + " Reclamation records");
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.log(Level.SEVERE, "Error loading Reclamation data", e);
                reclamations = new ArrayList<>();
            }
        } else {
            LOGGER.info("No existing Reclamation data file found. Starting with empty list.");
        }
    }

    /**
     * Saves Reclamation data to the JSON file
     */
    private void saveData() {
        try (Writer writer = new FileWriter(RECLAMATION_FILE)) {
            Type reclamationListType = new TypeToken<ArrayList<Reclamation>>() {
            }.getType();
            String jsonData = gson.toJson(reclamations, reclamationListType);
            writer.write(jsonData);
            LOGGER.info("Successfully saved " + reclamations.size() + " Reclamation records");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving Reclamation data", e);
        }
    }

    /**
     * Gets the personne associated with a reclamation
     */
    public Personne getPersonneForReclamation(Reclamation reclamation) {
        return personneService.getPersonneById(reclamation.getPersonneId());
    }

    /**
     * Type adapter for LocalDate
     */
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public JsonElement serialize(LocalDate localDate, Type type, JsonSerializationContext context) {
            return localDate != null ? new JsonPrimitive(formatter.format(localDate)) : JsonNull.INSTANCE;
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return null;
                }
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
            return localDateTime != null ? new JsonPrimitive(formatter.format(localDateTime)) : JsonNull.INSTANCE;
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return null;
                }
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
            return uuid != null ? new JsonPrimitive(uuid.toString()) : JsonNull.INSTANCE;
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return null;
                }
                return UUID.fromString(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Error parsing UUID: " + json.getAsString(), e);
            }
        }
    }

    /**
     * Type adapter for ReclamationStatus enum
     */
    private static class ReclamationStatusAdapter implements JsonSerializer<ReclamationStatus>, JsonDeserializer<ReclamationStatus> {

        @Override
        public JsonElement serialize(ReclamationStatus status, Type type, JsonSerializationContext context) {
            return status != null ? new JsonPrimitive(status.name()) : JsonNull.INSTANCE;
        }

        @Override
        public ReclamationStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return null;
                }
                return ReclamationStatus.valueOf(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Error parsing ReclamationStatus: " + json.getAsString(), e);
            }
        }
    }

    /**
     * Type adapter for ReclamationType enum
     */
    private static class ReclamationTypeAdapter implements JsonSerializer<ReclamationType>, JsonDeserializer<ReclamationType> {

        @Override
        public JsonElement serialize(ReclamationType type, Type typeOfSrc, JsonSerializationContext context) {
            return type != null ? new JsonPrimitive(type.name()) : JsonNull.INSTANCE;
        }

        @Override
        public ReclamationType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json.isJsonNull()) {
                    return null;
                }
                return ReclamationType.valueOf(json.getAsString());
            } catch (Exception e) {
                throw new JsonParseException("Error parsing ReclamationType: " + json.getAsString(), e);
            }
        }
    }
}
