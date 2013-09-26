package liquibase.ext.hibernate.database;

public enum ConfigType {
    HIBERNATE("hibernate"), EJB3("persistence"), SPRING("spring");

    private final String prefix;

    ConfigType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean matches(String url) {
        return url.startsWith(prefix + ":");
    }

    public static ConfigType forUrl(String url) {
        for (ConfigType cfg : values()) {
            if (cfg.matches(url))
                return cfg;
        }
        return null;
    }
}
