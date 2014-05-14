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
                <a href="${createLink(controller: 'representative', action: 'main')}" style="">
                    <g:message code="representativesPageLbl"/> <i class="fa fa-hand-o-right"></i></a>
            </li>
            <li>
                <a href="${createLink(controller: 'app', action: 'tools')}">
                    <g:message code="toolsLbl"/><i class="fa fa-cogs"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'subscriptionVS', action: 'feeds')}">
                    <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'app', action: 'contact')}"
                    style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                </a>
            </li>
        </ul>
    </nav>
</div>