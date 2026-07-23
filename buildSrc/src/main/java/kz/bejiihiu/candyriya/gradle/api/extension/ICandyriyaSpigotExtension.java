package kz.bejiihiu.candyriya.gradle.api.extension;

public interface ICandyriyaSpigotExtension {
    String getBukkitRef();
    void setBukkitRef(String ref);

    String getCraftBukkitRef();
    void setCraftBukkitRef(String ref);

    String getSpigotRef();
    void setSpigotRef(String ref);

    String getBuildDataRef();
    void setBuildDataRef(String ref);
}
