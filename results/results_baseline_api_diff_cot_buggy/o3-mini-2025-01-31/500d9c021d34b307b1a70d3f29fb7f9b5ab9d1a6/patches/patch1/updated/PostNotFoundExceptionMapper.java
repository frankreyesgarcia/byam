package com.example.config;

import com.example.domain.TaskNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import jakarta.mvc.Models;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject Logger log;

    @Inject
    Models models;

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        models.put("error", exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }
}
