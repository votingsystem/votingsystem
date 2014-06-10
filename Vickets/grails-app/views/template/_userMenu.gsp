<div id="navBarMainMenu" style="visibility:hidden;">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;">
                <div style="margin: 60px 0px 0px 0px;"><g:message code="sectionsLbl"/></div></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'transaction', action: 'listener')}">
                    <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'groupVS')}" style="">
                    <g:message code="selectGroupvsLbl"/> <i class="fa fa-list"></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'userVS', action: 'search')}"
                    style="color:#f9f9f9;"><g:message code="locateUserVSLbl"/> <i class="fa fa-users"></i>
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
