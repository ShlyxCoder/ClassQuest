package cn.org.shelly.edu.model.req;

import lombok.Data;

@Data
public class CommentReq {
    private Long gameId;
    private String comment;
    private Long studentId;
    private String studentName;
}
