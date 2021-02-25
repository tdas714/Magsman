package applied.ai.magsman;

public class Book {
    private String title, cover;
    private int lastpage, bookmark, totalpage, numplayed, id;

    public Book(int id, String title, String cover, int lastpage, int bookmark, int totalpage, int numplayed) {
        this.title = title;
        this.cover = cover;
        this.lastpage = lastpage;
        this.bookmark = bookmark;
        this.totalpage = totalpage;
        this.numplayed = numplayed;
        this.id = id;
    }
    public Book(){
    }
    // properties
    public void setID(int id) {
        this.id = id;
    }
    public int getID() {
        return this.id;
    }
//-----------------------------------------
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return this.title;
    }
//-----------------------------------------
    public void setCover(String cover) {
        this.cover = cover;
    }
    public String getCover() {
        return this.cover;
    }
//------------------------------------------
    public void setLastpage(int lastpage) {
        this.lastpage = lastpage;
    }
    public int getLastpage() {
        return this.lastpage;
    }
//------------------------------------------
    public void setBookmark(int bookmark) {
        this.bookmark = bookmark;
    }
    public int getBookmark() {
        return this.bookmark;
    }
//------------------------------------------
    public void setTotalpage(int totalpage) {
        this.totalpage = totalpage;
    }
    public int getTotalpage() {
        return this.totalpage;
    }
//-------------------------------------------
    public void setNumplayed(int numplayed) {
        this.numplayed = numplayed;
    }
    public int getNumplayed() {
        return this.numplayed;
    }

}
