package org.labkey.flow.controllers;

import org.labkey.api.action.*;
import org.labkey.api.view.TermsOfUseException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.RequestBasicAuthException;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.query.InvalidKeyException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.json.JSONObject;
import org.apache.log4j.Logger;

/** SAME as ExtFormAction, but with getView() on POST */
public abstract class FlowExtAction<FORM> extends ExtFormAction<FORM> implements NavTrailAction
{
    boolean isPost()
    {
        return "POST".equals(getViewContext().getRequest().getMethod());
    }

    public void checkPermissions() throws TermsOfUseException, UnauthorizedException
    {
        try
        {
            super.checkPermissions();
        }
        catch (TermsOfUseException e)
        {
            if (!isPost())
                throw e;
        }
        catch (UnauthorizedException e)
        {
            if (!isPost())
                throw e;
            if (!getViewContext().getUser().isGuest())
                throw e;
            else
                throw new RequestBasicAuthException();
        }
    }

    protected String getCommandClassMethodName()
    {
        return "execute";
    }

    public ModelAndView handleRequest() throws Exception
    {
        FORM form = null;
        BindException errors = null;

        try
        {
            String contentType = getViewContext().getRequest().getContentType();
            if (null != contentType && contentType.contains(ApiJsonWriter.CONTENT_TYPE_JSON))
            {
                _reqFormat = ApiResponseWriter.Format.JSON;
                JSONObject jsonObj = getJsonObject();

                form = getCommand();
                errors = populateForm(jsonObj, form);
            }
            else
            {
                if (null != getCommandClass())
                {
                    errors = defaultBindParameters(getCommand(), getPropertyValues());
                    form = (FORM)errors.getTarget();
                }
            }

            //validate the form
            validate(form, errors);

            if (isPost())
            {
                //if we had binding or validation errors,
                //return them without calling execute.
                if(null != errors && errors.hasErrors())
                    createResponseWriter().write((Errors)errors);
                else
                {
                    ApiResponse response = execute(form, errors);
                    if(null != response)
                        createResponseWriter().write(response);
                    else if (null != errors && errors.hasErrors())
                        createResponseWriter().write((Errors)errors);
                }
            }
            else
            {
                return getView(form, errors);
            }
        }
        catch (Exception e)
        {
            //don't log exceptions that result from bad inputs
            if(e instanceof IllegalArgumentException || e instanceof NotFoundException || e instanceof InvalidKeyException)
            {                                     
                createResponseWriter().write(e);
            }
            else
            {
                Logger.getLogger(ApiAction.class).warn("ApiAction exception: ", e);

                createResponseWriter().write(e);
            }
        }

        return null;
    } //handleRequest()


    public abstract ModelAndView getView(FORM form, BindException errors) throws Exception;

}
