package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mvc.annotation.Controller;
import javax.mvc.Models;
import javax.mvc.binding.BindingResult;
import javax.mvc.binding.BindingResult.Status;
import javax.mvc.security.CsrfProtected;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

    @Inject
    BindingResult bindingResult;

    @Inject
    Models models;

    @Inject
    AlertMessage flashMessage;

    @Inject
    Logger log;

    @GET
    public String get() {
        return "greeting.xhtml";
    }

    @POST
    @CsrfProtected
    public String post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        if (bindingResult.getStatus() == Status.FAILED) {
            AlertMessage alert = AlertMessage.danger("Validation voilations!");
            bindingResult.getAllErrors()
                    .stream()
                    .forEach((ParamError t) -> {
                        alert.addError(t.getParamName(), "", t.getMessage());
                    });
            models.put("errors", alert);
            log.info("mvc binding failed.");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:greeting";
    }

}