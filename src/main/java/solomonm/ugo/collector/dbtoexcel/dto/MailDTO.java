package solomonm.ugo.collector.dbtoexcel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import solomonm.ugo.collector.dbtoexcel.config.MessageConstant;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailDTO {
    @NotBlank
    @Email(message = "Sender" + MessageConstant.VALID_ERROR)
    private String sender;

    @NotBlank
    @Email(message = "Recipient" + MessageConstant.VALID_ERROR)
    private List<Object> recipients;

    @NotBlank(message = "Title" + MessageConstant.BLANK_ERROR)
    private String title;

    @NotBlank(message = "Content" + MessageConstant.BLANK_ERROR)
    private String content;

    @NotBlank(message = "Delivery type" + MessageConstant.BLANK_ERROR)
    private String deliveryType;
}
