public class AnimatedBoar {
    public String name;
    public String pluralName;
    public String rarity;
    public String description;
    public String staticFile;
    public String file;
    public Boolean isSB;
    public AnimatedBoar(String name, String pluralName, String rarity, String description, String file, String staticFile, boolean isSB) {
        super();
        this.name = name;
        this.pluralName = pluralName;
        this.rarity = rarity;
        this.description = description;
        this.file = file;
        this.staticFile = staticFile;
        this.isSB = isSB;
    }


    public String getName() {
        return this.name;
    }
    public String getRarity() {
        return this.rarity;
    }
    public String getDescrption() {
        return this.description;
    }
    public String getFile() { return this.file; }
    public String getStaticFile() { return this.staticFile; }
    public Boolean isSB() {
        return this.isSB;
    }

}

