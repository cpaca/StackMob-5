package uk.antiperson.stackmob.config;

import java.util.List;

public class ConfigList {

    private final List<?> list;
    private final String path;
    private final boolean inverted;
    private final ConfigFile configFile;

    public ConfigList(ConfigFile configFile, List<?> list, String path, boolean inverted) {
        this.configFile = configFile;
        this.list = list;
        this.path = path;
        this.inverted = inverted;
    }

    /**
     * List contains method which supports inverting lists.
     * @param tocheck object to check is in the list
     * @return whether this object is in the list.
     */
    public boolean contains(String tocheck) {
        if (inverted){
            return !rawContains(tocheck);
        }
        return rawContains(tocheck);
    }

    public boolean isInverted() {
        return inverted;
    }

    public boolean rawContains(String toCheck) {
        return list.contains(toCheck);
    }

    public List<Integer> asIntList() {
        return configFile.getIntegerList(path);
    }


}
