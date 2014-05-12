<div id="menu" class="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                    <g:message code="homeLbl"/> <i class="fa fa-home" style="color: #fdd302"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                    <g:message code="electionSystemLbl"/><i class="fa fa-envelope"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'eventVSManifest', action: 'main')}">
                    <g:message code="manifestSystemLbl"/><i class="fa fa-file-text"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'eventVSClaim', action: 'main')}">
                    <g:message code="claimSystemLbl"/><i class="fa fa-exclamation-triangle"></i>
                </a>
            </li>
            <li>
                <a href="#"><i class="fa fa-users"></i><g:message code="representativesPageLbl"/></a>
                <h2><i class="fa fa-users"></i><g:message code="representativesPageLbl"/></h2>
                <ul>
                    <li>
                        <a href="${createLink(controller: 'representative', action: 'main')}" style="">
                            <g:message code="selectRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i></a>
                    </li>
                    <li>
                        <a href="#"><g:message code="toolsLbl"/><i class="fa fa-cogs"></i></a>
                        <h2><g:message code="toolsLbl"/><i class="fa fa-cogs"></i></h2>
                        <ul>
                            <li>
                                <a href="${createLink(controller:'representative', action:'newRepresentative')}" style="">
                                    <g:message code="newRepresentativeLbl"/> <i class="fa fa-plus"></i></a>
                            </li>
                            <li>
                                <a href="${createLink(controller: 'representative', action: 'edit')}" style="">
                                    <g:message code="editRepresentativeLbl"/> <i class="fa fa-pencil"></i>
                                </a>
                            </li>
                            <li>
                                <a href="${createLink(controller:'representative', action:'remove')}" style="">
                                    <g:message code="removeRepresentativeLbl"/> <i class="fa fa-minus"></i></a>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
            <li>
                <a href="#"><i class="fa fa-pencil-square-o"></i><g:message code="publishDocumentLbl"/></a>
                <h2><i class="fa fa-pencil-square-o"></i><g:message code="publishDocumentLbl"/></h2>
                <ul>
                    <li>
                        <a href="${createLink(controller: 'editor', action: 'manifest')}" style="font-weight: normal;">
                            <g:message code="publishManifestLbl"/><i class="fa fa-certificate"></i></a>
                    </li>
                    <li>
                        <a href="${createLink(controller: 'editor', action: 'claim')}" style="font-weight: normal;">
                            <g:message code="publishClaimLbl"/> <i class="fa fa-exclamation-triangle"></i>
                        </a>
                    </li>
                    <li>
                        <a href="${createLink(controller: 'editor', action: 'vote')}" style="font-weight: normal;">
                            <g:message code="publishVoteLbl"/> <i class="fa fa-envelope"></i>
                        </a>
                    </li>
                </ul>
            </li>
            <li>
                <a href="${createLink(controller: 'subscriptionVS', action: 'feeds')}">
                    <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                </a>
            </li>
            <li>
                <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                    style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                </a>
            </li>
        </ul>
    </nav>
</div>