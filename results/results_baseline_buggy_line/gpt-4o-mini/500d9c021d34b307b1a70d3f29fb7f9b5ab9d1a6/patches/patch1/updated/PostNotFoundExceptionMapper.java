package com.example.config;

import com.example.domain.TaskNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.mvc.ModelAndView; // Updated import

/**
 *
 * @author hantsy
 */
@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject Logger log;
    //private static Logger log = Logger.getLogger(PostNotFoundExceptionMapper.class.getName());

    @Inject
    ModelAndView modelAndView; // Updated variable type

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        modelAndView.addObject("error", exception.getMessage()); // Updated method
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }

}