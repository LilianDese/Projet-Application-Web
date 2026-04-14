package pack;

public class GameCreateRequest {

    private String name;
    private Long creatorId;

    public GameCreateRequest() {
        // jackson
    }

    public GameCreateRequest(String name, Long creatorId) {
        this.name = name;
        this.creatorId = creatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }
}
