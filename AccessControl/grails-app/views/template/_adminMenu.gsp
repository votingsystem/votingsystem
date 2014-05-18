<div id="menu" class="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                    <g:message code="electionSystemLbl"/><i class="fa fa-envelope"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'editor', action: 'vote')}" style="font-weight: normal;">
                    <g:message code="publishVoteLbl"/><i class="fa fa-envelope"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'eventVSManifest', action: 'main')}">
                    <g:message code="manifestSystemLbl"/><i class="fa fa-file-text"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'editor', action: 'manifest')}" style="font-weight: normal;">
                    <g:message code="publishManifestLbl"/><i class="fa fa-file-text"></i></a>
            </li>
            <li>
                <a href="${createLink(controller: 'eventVSClaim', action: 'main')}">
                    <g:message code="claimSystemLbl"/><i class="fa fa-exclamation-triangle"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'editor', action: 'claim')}" style="font-weight: normal;">
                    <g:message code="publishClaimLbl"/> <i class="fa fa-exclamation-triangle"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'representative', action: 'main')}" style="">
                    <g:message code="representativesPageLbl"/> <i class="fa fa-hand-o-right"></i></a>
            </li>
            <li>
                <a href="${createLink(controller:'representative', action:'newRepresentative')}" style="">
                    <g:message code="newRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i></a>
            </li>
            <li>
                <a href="${createLink(controller: 'representative', action: 'edit')}" style="">
                    <g:message code="editRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller:'representative', action:'remove')}" style="">
                    <g:message code="removeRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i></a>
            </li>
        </ul>
    </nav>
</div>

<g:include view="/include/dialog/windowAlertModal.gsp"/>