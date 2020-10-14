/*
 * Copyright (c) 2002-2018, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.workflow.modules.forms.web.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.forms.business.Control;
import fr.paris.lutece.plugins.forms.business.ControlHome;
import fr.paris.lutece.plugins.forms.business.ControlType;
import fr.paris.lutece.plugins.forms.business.FormDisplay;
import fr.paris.lutece.plugins.forms.business.FormDisplayHome;
import fr.paris.lutece.plugins.forms.business.FormQuestionResponse;
import fr.paris.lutece.plugins.forms.business.FormResponse;
import fr.paris.lutece.plugins.forms.business.FormResponseStep;
import fr.paris.lutece.plugins.forms.business.Question;
import fr.paris.lutece.plugins.forms.business.QuestionHome;
import fr.paris.lutece.plugins.forms.business.Step;
import fr.paris.lutece.plugins.forms.business.StepHome;
import fr.paris.lutece.plugins.forms.business.TransitionHome;
import fr.paris.lutece.plugins.forms.service.EntryServiceManager;
import fr.paris.lutece.plugins.forms.util.FormsConstants;
import fr.paris.lutece.plugins.forms.web.entrytype.DisplayType;
import fr.paris.lutece.plugins.forms.web.entrytype.IEntryDataService;
import fr.paris.lutece.plugins.genericattributes.business.GenericAttributeError;
import fr.paris.lutece.plugins.genericattributes.business.Response;
import fr.paris.lutece.plugins.workflow.modules.forms.business.EditFormResponseTaskHistory;
import fr.paris.lutece.plugins.workflow.modules.forms.service.task.IEditFormResponseTaskHistoryService;
import fr.paris.lutece.plugins.workflow.modules.forms.service.task.IEditFormResponseTaskService;
import fr.paris.lutece.plugins.workflow.modules.forms.service.task.IFormsTaskService;
import fr.paris.lutece.plugins.workflowcore.service.task.ITask;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.util.html.HtmlTemplate;

/**
 * This class represents a component for the task {@link fr.paris.lutece.plugins.workflow.modules.forms.service.task.EditFormResponseTask EditFormResponseTask}
 *
 */
public class EditFormResponseTaskComponent extends AbstractFormResponseTaskComponent
{
    // Messages
    private static final String MESSAGE_ERROR = "module.workflow.forms.error.task.editFormResponse";

    // Templates
    private static final String TEMPLATE_TASK_FORM_EDITRESPONSE_HISTORY = "admin/plugins/workflow/modules/forms/task_forms_editresponse_history.html";

    private final IFormsTaskService _formsTaskService;
    private final IEditFormResponseTaskService _editFormResponseTaskService;
    private final IEditFormResponseTaskHistoryService _editFormResponseTaskHistoryService;

    /**
     * Constructor
     * 
     * @param formsTaskService
     *            the form task service
     * @param editFormResponseTaskService
     *            the edit form response task service
     */
    @Inject
    public EditFormResponseTaskComponent( IFormsTaskService formsTaskService, IEditFormResponseTaskService editFormResponseTaskService,
            IEditFormResponseTaskHistoryService editFormResponseTaskHistoryService )
    {
        super( );

        _formsTaskService = formsTaskService;
        _editFormResponseTaskService = editFormResponseTaskService;
        _editFormResponseTaskHistoryService = editFormResponseTaskHistoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doValidateTask( int nIdResource, String strResourceType, HttpServletRequest request, Locale locale, ITask task )
    {
        String strErrorUrl = null;

        FormResponse formResponse = _formsTaskService.findFormResponseFrom( nIdResource, strResourceType );
        List<Question> listQuestion = _editFormResponseTaskService.findQuestionsToEdit( formResponse );
        GenericAttributeError error = validateQuestions( listQuestion, request, formResponse.getFormId( ) );

        if ( error != null )
        {
            strErrorUrl = buildErrorUrl( error, request );
        }

        return strErrorUrl;
    }

    /**
     * Validates the specified questions
     * 
     * @param listQuestion
     *            the questions to validate
     * @param request
     *            the request
     * @param formId 
     * @return a {@code GenericAttributeError} if the validation fails, {@code null} otherwise
     */
    private GenericAttributeError validateQuestions( List<Question> listQuestion, HttpServletRequest request, int formId )
    {
        GenericAttributeError error = null;

        List<FormQuestionResponse> listFormQuestionResponse = new ArrayList<>( );

        for ( Question question : listQuestion )
        {
            IEntryDataService entryDataService = EntryServiceManager.getInstance( ).getEntryDataService( question.getEntry( ).getEntryType( ) );
            FormQuestionResponse formQuestionResponse = entryDataService.createResponseFromRequest( question, request, true );
            listFormQuestionResponse.add( formQuestionResponse );
        }

        for ( FormQuestionResponse formQuestionResponse : listFormQuestionResponse )
        {
            if ( !isResponseValid( listFormQuestionResponse, formQuestionResponse, formId ) )
            {
                error = formQuestionResponse.getError( );
                break;
            }
        }

        return error;
    }
    
    /**
     * Validate question.
     *
     * @param question
     *            the question
     * @param formId
     * @return true, if successful
     */
    private boolean isResponseValid( List<FormQuestionResponse> listFormQuestionResponse, FormQuestionResponse formQuestionResponse, int formId )
    {
        if ( !formQuestionResponse.hasError( ) || ( formQuestionResponse.hasError( ) && !formQuestionResponse.getError( ).isMandatoryError( ) ) )
        {
            return true;
        }

        Control controlConditionnalDisplay = getControlConditionnalDisplay( formQuestionResponse, formId );

        // No Conditional validation
        if ( controlConditionnalDisplay == null )
        {
            return true;
        } else
        {
            for ( int questionId : controlConditionnalDisplay.getListIdQuestion( ) )
            {
                Question questionConditional = QuestionHome.findByPrimaryKey( questionId );
                List<Response> listResponses = findResponses( listFormQuestionResponse, questionConditional );
                for ( Response response : listResponses )
                {
                    String conditionalExpectedValue = controlConditionnalDisplay.getValue( );
                    if ( conditionalExpectedValue.equals( response.getToStringValueResponse( ) ) || conditionalExpectedValue.equals( String.valueOf( response.getField( ).getIdField( ) ) ) )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Gets the control conditionnal display.
     *
     * @param formQuestionResponse
     *            the form question response
     * @param formId
     *            the form id
     * @return the control conditionnal display
     */
    private Control getControlConditionnalDisplay( FormQuestionResponse formQuestionResponse, int formId )
    {
        Question question = formQuestionResponse.getQuestion( );
        FormDisplay formDisplay = FormDisplayHome.getFormDisplayByFormStepAndComposite( formId, formQuestionResponse.getIdStep( ), question.getId( ) );

        List<Control> listConditionalControl = ControlHome.getControlByControlTargetAndType( formDisplay.getId( ), ControlType.CONDITIONAL );
        Control controlConditionnalDisplay = null;

        if ( !listConditionalControl.isEmpty( ) )
        {
            controlConditionnalDisplay = listConditionalControl.get( 0 );
        }
        return controlConditionnalDisplay;
    }
    
    /**
     * Finds the responses associated to this instance among the specified list of form question responses
     * 
     * @param listFormQuestionResponse
     *            the list of form question responses
     * @return the responses
     */
    private List<Response> findResponses( List<FormQuestionResponse> listFormQuestionResponse, Question question )
    {
        List<Response> listResponse = new ArrayList<>( );

        if ( listFormQuestionResponse != null )
        {
            for ( FormQuestionResponse formQuestionResponse : listFormQuestionResponse )
            {
                Question formQuestion = formQuestionResponse.getQuestion( );

                if ( question.getId( ) == formQuestion.getId( ) && question.getIterationNumber( ) == formQuestion.getIterationNumber( ) )
                {
                    listResponse = formQuestionResponse.getEntryResponse( );
                    break;
                }
            }
        }

        return listResponse;
    }

    /**
     * Builds the error URL
     * 
     * @param error
     *            the error
     * @param request
     *            the request
     * @return the error URL
     */
    private String buildErrorUrl( GenericAttributeError error, HttpServletRequest request )
    {
        Object [ ] listMessageParameters = {
            error.getErrorMessage( ),
        };

        return AdminMessageService.getMessageUrl( request, MESSAGE_ERROR, listMessageParameters, AdminMessage.TYPE_STOP );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayTaskForm( int nIdResource, String strResourceType, HttpServletRequest request, Locale locale, ITask task )
    {
        FormResponse formResponse = _formsTaskService.findFormResponseFrom( nIdResource, strResourceType );
        List<Question> listQuestion = _editFormResponseTaskService.findQuestionsToEdit( formResponse );

        Set<Integer> listStepId = new HashSet<>( );
        List<Step> listStep = new ArrayList<>( );

        for ( Question question : listQuestion )
        {
            if ( !listStepId.contains( question.getIdStep( ) ) )
            {
                listStepId.add( question.getIdStep( ) );
            }
        }

        List<FormResponseStep> listFormResponseStep = formResponse.getSteps( );
        List<Integer> listStepsOrdered = new ArrayList<>( );

        for ( FormResponseStep formResponseStep : listFormResponseStep )
        {
            listStepsOrdered.add( formResponseStep.getStep( ).getId( ) );
        }

        // Filter only the steps which contains question to edit in BO
        listStepsOrdered.removeIf( stepId -> !listStepId.contains( stepId ) );
        
        //Add the steps that are editable but not in the actuel form response flow
        for ( Integer nIdStep : listStepId )
        {
            if ( !listStepsOrdered.contains( nIdStep ) && TransitionHome.getTransitionsListFromStep( nIdStep ).isEmpty( ) )
            {
                listStepsOrdered.add( nIdStep );
            }
        }

        for ( Integer nIdStep : listStepsOrdered )
        {
            listStep.add( StepHome.findByPrimaryKey( nIdStep ) );
        }

        List<String> listStepDisplayTree = _formsTaskService.buildFormStepDisplayTreeList( request, listStep, listQuestion, formResponse, DisplayType.EDITION_BACKOFFICE );

        Map<String, Object> model = new HashMap<>( );
        model.put( MARK_STEP_LIST, listStepDisplayTree );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_FORM, locale, model );

        return template.getHtml( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayTaskInformation( int nIdHistory, HttpServletRequest request, Locale locale, ITask task )
    {
        Map<String, Object> model = new HashMap<>( );
        List<EditFormResponseTaskHistory> listEditFormResponseTaskHistory = _editFormResponseTaskHistoryService.load( nIdHistory, task.getId( ) );

        model.put( FormsConstants.MARK_QUESTION_LIST_RESPONSES, listEditFormResponseTaskHistory );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_FORM_EDITRESPONSE_HISTORY, locale, model );
        return template.getHtml( );
    }
    
    @Override
    public String getDisplayConfigForm( HttpServletRequest request, Locale locale, ITask task ) 
    {
    	return null;
    }

}
