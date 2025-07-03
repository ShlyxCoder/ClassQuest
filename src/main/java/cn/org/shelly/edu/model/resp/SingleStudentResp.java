package cn.org.shelly.edu.model.resp;

import cn.org.shelly.edu.model.pojo.ClassStudent;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SingleStudentResp {

    @Schema(description = "学号")
    private String sno;


    @Schema(description = "姓名")
    private String name;


    @Schema(description = "班级ID")
    private Long cid;
    @Schema(description = "学生id")
    private Long id;

    public static SingleStudentResp toResp(ClassStudent classStudent) {
        return new SingleStudentResp()
                .setCid(classStudent.getCid())
                .setId(classStudent.getId())
                .setName(classStudent.getName())
                .setSno(classStudent.getSno());
    }
}
