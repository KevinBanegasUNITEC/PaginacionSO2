public class Reference {
    private String page;
    private char type;

    public Reference(String page, char type) {
        this.page = page;
        this.type = type;
    }
    public String getPage() {
        return page;
    }
    public char getType() {
        return type;
    }
    public void setPage(String page) {
        this.page = page;
    }
    public void setType(char type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return page + " " + type;
    }
}
