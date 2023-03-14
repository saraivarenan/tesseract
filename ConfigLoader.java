

import static java.io.File.separator;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.util.Collections.emptyMap;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter.Config;
import java.util.HashMap;
import java.util.Map;


import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigLoader {

	private static final Yaml YAML = new Yaml();

	private static ConfigLoader instance;

	private Configuration config;

	private Map<String, Object> configMap = new HashMap<>();

	private ObjectMapper om = new ObjectMapper();

	private ConfigLoader() {
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static synchronized Configuration getConfig() {
		return getConfig(false);
	}

	public static synchronized Configuration getConfig(boolean forceReload) {
		return getInstance(forceReload).config;
	}

	private static ConfigLoader getInstance() { return getInstance(false); }

	private static ConfigLoader getInstance(boolean forceReload) {
		if (instance != null && !forceReload) return instance;

		instance = new ConfigLoader();

		System.out.println("Ambiente: " + getEnv("profile", "Não configurado"));

		Map<String, Object> loadedConfig = loadConfig();

		Map<String, Object> solvedData = loadedConfig == null ? null : MapUtils.resolveParameters(loadedConfig);

		instance.config = solvedData == null ? new Configuration() : getInstance().om.convertValue(solvedData, Configuration.class);

		instance.configMap = MapUtils.resolveToNestedProps(solvedData);

		return instance;
	}

	private static Map<String, Object> loadConfig() {
		Map<String, Object> applicationConfig = loadConfigFromApplication();

		Map<String, Object> userConfig = loadConfigFromUser();

		if (applicationConfig == null && userConfig == null) return null;

		if (applicationConfig != null && userConfig != null) return MapUtils.mergeMaps(applicationConfig, userConfig);

		if (userConfig != null) return userConfig;

		return applicationConfig;
	}

	private static Map<String, Object> loadConfigFromUser() {
		File file = new File(getProperty("user.home") + separator + ".tesseract" + separator + "config.yml");

		if (!file.exists()) return emptyMap();

		try (InputStream is = new FileInputStream(file);) {
			return YAML.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, Object> loadConfigFromApplication() {
		Map<String, Object> data = loadFile("application.yml");

		String profile = getEnv("profile");

		if (profile == null) return data;

		Map<String, Object> profileData = loadFile("application-" + profile + ".yml");

		return MapUtils.mergeMaps(data, profileData);
	}

	private static Map<String, Object> loadFile(String fileName) {
		InputStream file = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName);

		if (file == null) return null;

		return YAML.load(file);
	}

	public static Map<String, Object> getEnvStartWith(String property) {
		String startWith = property + ".";

		Map<String, Object> values = new HashMap<>();

		getInstance().configMap.forEach((key, value) -> {
			if (key.startsWith(startWith)) values.put(key.substring(startWith.length()), value);
		});

		return values;
	}

	public static String getEnv(String property) {
		return getEnv(property, null);
	}

	public static String getEnv(String property, boolean forceRefesh) {
		return getEnv(property, null, forceRefesh);
	}

	public static String getEnv(String property, String defaultValue) {
		return getEnv(property, defaultValue, false);
	}

	public static String getEnv(String property, String defaultValue, boolean forceRefesh) {
		String env = getenv(property);

		if (env != null) return env;

		env = getProperty(property);

		if (env != null) return env;

		if (getInstance(forceRefesh).configMap != null) {
			Object vl = getInstance().configMap.get(property);
			env = vl != null ? valueOf(vl) : null;
		}

		if (env != null) return env;

		return defaultValue;
	}

	public static void setEnv(String key, String value) {
		getInstance().configMap.put(key, value);
	}
}
