package com.ttclub.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "blog_images")
public class BlogImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private BlogPost post;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BlogPost getPost() { return post; }
    public void setPost(BlogPost post) { this.post = post; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }

    public short getSortOrder() { return sortOrder; }
    public void setSortOrder(short sortOrder) { this.sortOrder = sortOrder; }
}
