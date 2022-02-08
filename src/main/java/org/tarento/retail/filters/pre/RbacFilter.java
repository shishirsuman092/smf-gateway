package org.tarento.retail.filters.pre;

import static org.tarento.retail.constants.RequestContextConstants.ERROR_CODE_KEY;
import static org.tarento.retail.constants.RequestContextConstants.ERROR_MESSAGE_KEY;
import static org.tarento.retail.constants.RequestContextConstants.RBAC_BOOLEAN_FLAG_NAME;
import static org.tarento.retail.constants.RequestContextConstants.USER_INFO_KEY;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.tarento.retail.contract.Action;
import org.tarento.retail.contract.User;
import org.tarento.retail.exceptions.zuulExceptions.RbacException;
import org.tarento.retail.util.ExceptionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 *  5th pre filter to get executed.
 *  Filter gets executed if the RBAC flag is enabled. Returns an error if the URI is not present in the authorized action list.
 */
public class RbacFilter extends ZuulFilter{

    private static final String FORBIDDEN_MESSAGE = "Not authorized to access this resource";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String filterType() {return "pre";}

    @Override
    public int filterOrder() {return 4;}

    @Override
    public boolean shouldFilter() {
    	if("OPTIONS".equals(RequestContext.getCurrentContext().getRequest().getMethod())) { 
    		return false; 
    	}
        RequestContext ctx = RequestContext.getCurrentContext();
        return ctx.getBoolean(RBAC_BOOLEAN_FLAG_NAME);
    }

    @Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();

		final boolean isIncomingURIInAuthorizedActionList = isIncomingURIInAuthorizedActionList(ctx);
		if (isIncomingURIInAuthorizedActionList) {
			ctx.set("RbacAvailable", Boolean.TRUE);
			return null;
		} else {
			ctx.set("RbacAvailable", Boolean.FALSE);
			ExceptionUtils.raiseCustomException(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
		}

		return null;
	}

    private boolean isIncomingURIInAuthorizedActionList(RequestContext ctx) {
        String requestUri = ctx.getRequest().getRequestURI();
        User user = (User) ctx.get(USER_INFO_KEY);
        if (user == null) {
            ExceptionUtils.raiseCustomException(HttpStatus.UNAUTHORIZED, "User information not found. Can't execute RBAC filter");
        }
        List<Action> actions = user.getActions();
        return actions.stream()
                .anyMatch(action -> isActionMatchingIncomingURI(requestUri, action));
    }

    private boolean isActionMatchingIncomingURI(String requestUri, Action action) {
        if(action.hasDynamicFields()) {
            return requestUri.matches(action.getRegexUrl());
        }
        return requestUri.equals(action.getUrl());
    }


    private void abortWithStatus(RequestContext ctx, HttpStatus status, String message) {
        ctx.set(ERROR_CODE_KEY, status.value());
        ctx.set(ERROR_MESSAGE_KEY, message);
        ctx.set("error.exception", new RbacException("Role does not have access to this URL"));
        ctx.setSendZuulResponse(false);
    }
    
    private void abortWithStatus(int status, String message, Exception ex) {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(ERROR_CODE_KEY, status);
        ctx.set(ERROR_MESSAGE_KEY, message);
        ctx.set("error.exception", ex);
        ctx.set(RBAC_BOOLEAN_FLAG_NAME, Boolean.FALSE); 
        ctx.setSendZuulResponse(false);
    }
}
