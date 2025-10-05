package com.ttclub.backend.dto;


public class ProductImageDto {

    /* Database identifier (null for brand-new images) */
    private Long   id;

    /* Publicly resolvable URL (S3, CloudFront, etc.) */
    private String url;

    /* Accessible alt-text shown to screen-readers */
    private String altText;

    /* Hero / thumbnail indicator */
    private boolean primary;

    /* Manual ordering inside the gallery (0-based) */
    private short  sortOrder;

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getUrl()                       { return url; }
    public void setUrl(String url)               { this.url = url; }

    public String getAltText()                   { return altText; }
    public void setAltText(String altText)       { this.altText = altText; }

    public boolean isPrimary()                   { return primary; }
    public void setPrimary(boolean primary)      { this.primary = primary; }

    public short getSortOrder()                  { return sortOrder; }
    public void setSortOrder(short sortOrder)    { this.sortOrder = sortOrder; }
}
