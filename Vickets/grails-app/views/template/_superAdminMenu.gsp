<div id="menu" class="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'transaction', action: 'listener')}">
                    <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'userVS', action: 'newVicketSource')}" style="color:#f9f9f9;">
                    <g:message code="newVicketSourceLbl"/><i class="fa fa-university" style=""></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'certificateVS', action: 'addCertificateAuthority')}"
                    style="color:#f9f9f9;"><g:message code="newCAAuthorityLbl"/> <i class="fa fa-certificate" style=""></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'certificateVS', action: 'certs')}"
                    style="color:#f9f9f9;"><g:message code="locateCertLbl"/> <i class="fa fa-users"></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'userVS', action: 'search')}"
                    style="color:#f9f9f9;"><g:message code="locateUserVSLbl"/> <i class="fa fa-users"></i>
                </a>
            </li>
        </ul>
    </nav>
</div>
<asset:script>

</asset:script>