package Entities;

/**
 * Created by vibhor.go on 01/05/17.
 */

public class URLBean
{
    private String url;
    private String title;
    private String heading;
    private String content;
    private TermVector titleTermVec;
    private TermVector contenTermVec;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TermVector getTitleTermVec() {
        return titleTermVec;
    }

    public TermVector getContenTermVec() {
        return contenTermVec;
    }

    public void calculateTermVecs()
    {
        titleTermVec= new TermVector(title);
        contenTermVec= new TermVector(content);
    }



}
