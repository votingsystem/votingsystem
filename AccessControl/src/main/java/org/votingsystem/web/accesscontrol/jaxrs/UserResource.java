package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeBean;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/user")
public class UserResource {

    @Inject RepresentativeBean representativeBean;

    @Path("/nif/{nif}/representationState") @GET
    public Response state(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        RepresentationStateDto result = representativeBean.checkRepresentationState(nifReq);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(result)).type(MediaTypeVS.JSON).build();
    }

}
