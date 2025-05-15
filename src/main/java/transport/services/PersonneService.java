package transport.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import transport.core.Employe;
import transport.core.Fonction;
import transport.core.Personne;
import transport.core.Usager;

/**
 * Service class to manage Personne entities and their subclasses (Employe,
 * Usager) Handles JSON serialization/deserialization and CRUD operations
 */
public class PersonneService {

    private static final Logger LOGGER = Logger.getLogger(PersonneService.class.getName());
    private static final String DATA_DIRECTORY = "data";
    private static final String PERSONNE_FILE = DATA_DIRECTORY + "/personnes.json";
    private List<Personne> personnes = new ArrayList<>();
    private final Gson gson;

    public PersonneService() {
        // Create custom GSON instance with type adapters for LocalDate, UUID and Personne
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeAdapter(Personne.class, new PersonneAdapter())
                // Register a type adapter for List<Personne> to handle polymorphism correctly
                .registerTypeAdapter(new TypeToken<ArrayList<Personne>>() {
                }.getType(), new PersonneListAdapter())
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
     * Retrieves all Personne entities
     *
     * @return A copy of the list of all personnes
     */
    public List<Personne> getAllPersonnes() {
        return new ArrayList<>(personnes);
    }

    /**
     * Retrieves a specific Personne by ID
     *
     * @param id The UUID of the Personne to retrieve
     * @return The Personne object or null if not found
     */
    public Personne getPersonneById(UUID id) {
        return personnes.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Saves a Personne entity (create or update)
     *
     * @param personne The Personne object to save
     */
    public void savePersonne(Personne personne) {
        if (personne == null) {
            LOGGER.warning("Attempted to save null Personne object");
            return;
        }

        // If the personne doesn't have an ID yet, generate one
        if (personne.getId() == null) {
            personne.setId(UUID.randomUUID());
        }

        // Check if personne already exists
        boolean exists = false;
        for (int i = 0; i < personnes.size(); i++) {
            if (personnes.get(i).getId().equals(personne.getId())) {
                personnes.set(i, personne);
                exists = true;
                break;
            }
        }

        if (!exists) {
            personnes.add(personne);
        }

        saveData();
    }

    /**
     * Deletes a Personne by ID
     *
     * @param id The UUID of the Personne to delete
     * @return true if found and deleted, false otherwise
     */
    public boolean deletePersonne(UUID id) {
        boolean removed = personnes.removeIf(p -> p.getId().equals(id));
        if (removed) {
            saveData();
        }
        return removed;
    }

    /**
     * Loads Personne data from the JSON file
     */
    private void loadData() {
        File file = new File(PERSONNE_FILE);

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                // Use Gson's TypeToken for proper generic type resolution
                Type personneListType = new TypeToken<ArrayList<Personne>>() {
                }.getType();
                List<Personne> loadedPersonnes = gson.fromJson(reader, personneListType);

                if (loadedPersonnes != null) {
                    // Validate loaded data
                    List<Personne> validPersonnes = new ArrayList<>();
                    for (Personne p : loadedPersonnes) {
                        if (p != null && p.getId() != null) {
                            validPersonnes.add(p);
                        } else {
                            LOGGER.warning("Skipped invalid Personne record during loading");
                        }
                    }
                    personnes = validPersonnes;
                } else {
                    personnes = new ArrayList<>();
                }

                LOGGER.info("Loaded " + personnes.size() + " Personne records");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading Personne data", e);
                personnes = new ArrayList<>();
            } catch (JsonSyntaxException e) {
                LOGGER.log(Level.SEVERE, "Error parsing Personne JSON data", e);
                personnes = new ArrayList<>();
                // Backup the corrupted file for investigation
                backupCorruptedFile();
            }
        } else {
            LOGGER.info("No existing Personne data file found. Starting with empty list.");
        }
    }

    /**
     * Saves Personne data to the JSON file
     */
    private void saveData() {
        try (Writer writer = new FileWriter(PERSONNE_FILE)) {
            // Log the data before serialization
            LOGGER.fine("Serializing " + personnes.size() + " Personne records");

            // Use our custom type token to ensure the adapter is used
            Type personneListType = new TypeToken<ArrayList<Personne>>() {
            }.getType();

            // Convert to JSON and write to file
            String jsonData = gson.toJson(personnes, personneListType);
            LOGGER.finest("JSON output: " + jsonData);
            writer.write(jsonData);

            LOGGER.info("Successfully saved " + personnes.size() + " Personne records");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving Personne data", e);
        }
    }

    /**
     * Creates a backup of the corrupted data file
     */
    private void backupCorruptedFile() {
        File source = new File(PERSONNE_FILE);
        if (source.exists()) {
            File backup = new File(PERSONNE_FILE + ".corrupted." + System.currentTimeMillis());
            try {
                copyFile(source, backup);
                LOGGER.info("Created backup of corrupted data: " + backup.getName());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to backup corrupted data file", e);
            }
        }
    }

    /**
     * Helper method to copy a file
     */
    private void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
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
     * Type adapter for Personne and its subclasses (Employe, Usager) Handles
     * polymorphic serialization/deserialization
     */
    private static class PersonneAdapter implements JsonSerializer<Personne>, JsonDeserializer<Personne> {

        @Override
        public JsonElement serialize(Personne personne, Type type, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            // Determine the type of person
            String className;
            if (personne instanceof Employe) {
                className = "Employe";
            } else if (personne instanceof Usager) {
                className = "Usager";
            } else {
                throw new JsonParseException("Unknown Personne type: " + personne.getClass().getName());
            }
            System.out.println("Serializing Personne of type: " + className);

            // Add type at the root level for clearer deserialization
            result.add("type", new JsonPrimitive(className));

            // Serialize all fields from the concrete class
            JsonObject personneData = new JsonObject();

            // Add common fields from Personne
            personneData.add("id", context.serialize(personne.getId()));
            personneData.add("name", new JsonPrimitive(personne.getName()));
            personneData.add("familyName", new JsonPrimitive(personne.getFamilyName()));
            personneData.add("birthDate", context.serialize(personne.getBirthDate()));
            personneData.add("hasHandicap", new JsonPrimitive(personne.hasHandicap()));

            // Add specific fields based on the concrete type
            if (personne instanceof Employe) {
                Employe employe = (Employe) personne;
                personneData.add("matricule", new JsonPrimitive(employe.getMatricule() != null
                        ? employe.getMatricule() : ""));

                if (employe.getFonction() != null) {
                    personneData.add("fonction", new JsonPrimitive(employe.getFonction().name()));
                } else {
                    personneData.add("fonction", JsonNull.INSTANCE);
                }
            }

            result.add("data", personneData);
            return result;
        }

        @Override
        public Personne deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();

                if (!jsonObject.has("type") || !jsonObject.has("data")) {
                    throw new JsonParseException("Invalid Personne JSON structure: missing type or data");
                }

                String type = jsonObject.get("type").getAsString();
                JsonObject data = jsonObject.get("data").getAsJsonObject();

                // Extract common fields
                UUID id = context.deserialize(data.get("id"), UUID.class);
                String name = data.get("name").getAsString();
                String familyName = data.get("familyName").getAsString();
                LocalDate birthDate = context.deserialize(data.get("birthDate"), LocalDate.class);
                boolean hasHandicap = data.get("hasHandicap").getAsBoolean();

                Personne personne = null;

                switch (type) {
                    case "Employe":
                        String matricule = "";
                        if (data.has("matricule") && !data.get("matricule").isJsonNull()) {
                            matricule = data.get("matricule").getAsString();
                        }

                        Fonction fonction = null;
                        if (data.has("fonction") && !data.get("fonction").isJsonNull()) {
                            try {
                                fonction = Fonction.valueOf(data.get("fonction").getAsString());
                            } catch (IllegalArgumentException e) {
                                LOGGER.warning("Invalid fonction value: " + data.get("fonction").getAsString()
                                        + ". Using default value ADMINISTRATIF.");
                                fonction = Fonction.ADMINISTRATIF; // Default value
                            }
                        }

                        personne = new Employe(name, familyName, birthDate, hasHandicap, matricule, fonction);
                        break;

                    case "Usager":
                        personne = new Usager(name, familyName, birthDate, hasHandicap);
                        break;

                    default:
                        throw new JsonParseException("Unknown Personne type: " + type);
                }

                // Make sure to set the ID
                if (personne != null) {
                    personne.setId(id);
                }

                return personne;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deserializing Personne", e);
                throw new JsonParseException("Error deserializing Personne: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Type adapter for handling lists of Personne objects
     */
    private static class PersonneListAdapter implements JsonSerializer<List<Personne>>, JsonDeserializer<List<Personne>> {

        @Override
        public JsonElement serialize(List<Personne> personnes, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray jsonArray = new JsonArray();

            for (Personne personne : personnes) {
                // Create object with type information
                JsonObject personneObject = new JsonObject();

                // Determine the type
                String className;
                if (personne instanceof Employe) {
                    className = "Employe";
                } else if (personne instanceof Usager) {
                    className = "Usager";
                } else {
                    throw new JsonParseException("Unknown Personne type: " + personne.getClass().getName());
                }

                personneObject.add("type", new JsonPrimitive(className));

                // Add data field
                JsonObject dataObject = new JsonObject();

                // Common fields
                dataObject.add("id", context.serialize(personne.getId()));
                dataObject.add("name", new JsonPrimitive(personne.getName()));
                dataObject.add("familyName", new JsonPrimitive(personne.getFamilyName()));
                dataObject.add("birthDate", context.serialize(personne.getBirthDate()));
                dataObject.add("hasHandicap", new JsonPrimitive(personne.hasHandicap()));

                // Special fields for Employe
                if (personne instanceof Employe) {
                    Employe employe = (Employe) personne;
                    dataObject.add("matricule", new JsonPrimitive(
                            employe.getMatricule() != null ? employe.getMatricule() : ""));

                    if (employe.getFonction() != null) {
                        dataObject.add("fonction", new JsonPrimitive(employe.getFonction().name()));
                    } else {
                        dataObject.add("fonction", JsonNull.INSTANCE);
                    }
                }

                personneObject.add("data", dataObject);
                jsonArray.add(personneObject);
            }

            return jsonArray;
        }

        @Override
        public List<Personne> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            List<Personne> personnes = new ArrayList<>();
            JsonArray jsonArray = json.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                try {
                    // Use the existing PersonneAdapter logic for each element
                    Personne personne = context.deserialize(element, Personne.class);
                    personnes.add(personne);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error deserializing a Personne record, skipping: " + e.getMessage());
                }
            }

            return personnes;
        }
    }
}
