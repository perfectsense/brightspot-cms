---

# View System

---

### Model

Given a simple article class:

```java
public class Article extends Content {

    private String headline;

    public String getHeadline() {
        return headline;
    }

    public List<Module> getRightModules() {
        return Query.from(Module.class).selectAll();
    }
}
```

---

### Annotations

Old renderer annotations:

```java
@Renderer.Path(value = "/article.jsp", context = "left")
@Renderer.LayoutPath("/two-column-page.jsp")
public class Article ...
```

New view annotations:

```java
@ViewBinding(value = ArticleViewModel.class, types = { TwoColumnPageViewModel.LEFT_ELEMENT })
@ViewBinding(value = TwoColumnPageViewModel.class, types = { PageFilter.PAGE_VIEW_TYPE })
public class Article ...
```


---

### Rendering Article - Old

`Article.java`

```java
@Renderer.Path("/article.jsp")
public class Article ...
```

`article.jsp`

```jsp
<div class="article">
    <h1 class="headline"><cms:render value="${headline}" /></h1>
</div>
```

---

### Rendering Article - New

`Article.java`

```java
@ViewBinding(value = ArticleViewModel.class, types = { TwoColumnPageViewModel.LEFT_ELEMENT })
public class Article ...
```

`Article.hbs`

```hbs
<div class="Article">
    <h1 class="Article-headline">{{headline}}</h1>
</div>
```

`ArticleViewModel.java`

```java
// ArticleView is automatically generated based on Article.hbs
public class ArticleViewModel extends ViewModel<Article> implements ArticleView {

    public Object getHeadline() {
        return model.getHeadline();
    }
}
```

---

### Changing Article - Model

To change `headline` to rich text:

`Article.java`

```java
public class Article extends Content {

    @ToolUi.RichText
    private String headline;
}
```

---

### Changing Article - Old

To display rich text as is, remove `<c:out>` call:

`article.jsp`

```jsp
<div class="article">
    <h1 class="headline">${headline}</h1>
</div>
```

---

### Changing Article - New

To display rich text as is, return a `RawHtmlView` instance:

`ArticleViewModel.java`

```java
public class ArticleViewModel extends ViewModel<Article> implements ArticleView {

    public Object getHeadline() {
        return new RawHtmlView.Builder()
                .html(model.getHeadline())
                .build();
    }
}
```

---

### Article Pagination - Old

`article.jsp`

```jsp
<div class="article">
    <div class="body"><!-- Use ${param.page} --></div>
</div>
```

---

### Article Pagination - New

`ArticleViewModel.java`

```java
public class ArticleViewModel extends ViewModel<Article> implements ArticleView {

    @HttpParameter
    private int page;

    public Object getBody() {
        // Use page field.
    }
}
```

---

### Rendering Page - Old

`two-column-page.jsp`

```jsp
<!doctype html>
<html>
<body>
    <div class="left">
        <cms:render context="left" value="${mainContent}" />
    </div>
    <div class="right">
        <c:forEach items="${mainContent.rightModules}" var="module">
            <cms:render context="right" value="${module}" />
        </c:forEach>
    </div>
</body>
</html>
```

---

### Rendering Page - New Template

`TwoColumnPage.hbs`

```hbs
<!doctype html>
<html>
<body class="TwoColumnPage">
    <div class="TwoColumnPage-left">
        {{left}}
    </div>
    <div class="TwoColumnPage-right">
        {{#each right}}
            {{this}}
        {{/each}}
    </div>
</body>
</html>
```

---

### Rendering Page - New View Model

`TwoColumnPageViewModel.java`

```java
public class TwoColumnPageViewModel extends ViewModel<Object> implements TwoColumnPageView {

    public Object getLeft() {
        return createView(LEFT_ELEMENT, model);
    }

    public List<?> getRight() {
        if (model instanceof Article) {
            return ((Article) model)
                    .getRightModules()
                    .map(m -> createView(RIGHT_ELEMENT, m));

        } else {
            return null;
        }
    }
}
```

---

### Rendering Page - Left Rail

Old:

```jsp
    <div class="left">
        <cms:render context="left" value="${mainContent}" />
    </div>
```

New:

```hbs
    <div class="TwoColumnPage-left">
        {{left}}
    </div>
```

```java
    public Object getLeft() {
        return createView(LEFT_ELEMENT, model);
    }
```
