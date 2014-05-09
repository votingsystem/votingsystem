<div id="menu" class="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'app', action: 'index')}">
                    <g:message code="homeLbl"/> <i class="a fa fa-home" style="color: #fdd302"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'transaction', action: 'listener')}">
                    <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'groupVS', action: 'index')}" style="">
                    <g:message code="groupvsSectionLbl"/> <i class="fa fa-users"></i></i></a>
            </li>
            <li>
                <a href="${createLink(controller: 'app', action: 'tools')}">
                    <g:message code="toolsSectionLbl"/> <i class="fa fa-cogs"></i>
                </a>
            </li>
            <li>
                <a href="#">
                    <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                </a>
            </li>
            <li>
                <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                    style="color:#f9f9f9;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                </a>
            </li>
        </ul>
    </nav>
</div>