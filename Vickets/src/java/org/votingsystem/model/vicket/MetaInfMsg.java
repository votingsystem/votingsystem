package org.votingsystem.model.vicket;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MetaInfMsg {

    public static final String signature_EXCEPTION = "signature_EXCEPTION";
    public static final String signature_EXCEPTION_certPathValidator = "signature_EXCEPTION_certPathValidator";
    public static final String signature_ERROR_timestamp = "signature_ERROR_timestamp";
    public static final String signature_ERROR_timestampMissing = "signature_ERROR_timestampMissing";
    public static final String signature_ERROR_hashRepeated = "signature_ERROR_hashRepeated";

    public static final String user_ERROR_missingNif = "user_ERROR_missingNif";
    public static final String user_ERROR_missingCert = "user_ERROR_missingCert";
    public static final String user_ERROR_nif = "user_ERROR_nif";

    public static final String cancelVicketGroup_ERROR_userWithoutPrivilege = "cancelVicketGroup_ERROR_userWithoutPrivilege";
    public static final String cancelVicketGroup_ERROR_params = "cancelVicketGroup_ERROR_params";
    public static final String cancelVicketGroup_OK = "cancelGroup_OK_groupvs_id_";

    public static final String editVicketGroup_ERROR_userWithoutPrivileges = "editVicketGroup_ERROR_userWithoutPrivileges";
    public static final String editVicketGroup_ERROR_params = "editVicketGroup_ERROR_params";
    public static final String editVicketGroup_ERROR_id = "editVicketGroup_ERROR_id";
    public static final String editVicketGroup_OK = "editVicketGroup_OK_groupvs_id_";

    public static final String deActivateVicketGroupUser_OK = "deActivateVicketGroupUser_OK_";
    public static final String deActivateVicketGroupUser_ERROR_groupUserAlreadyCancelled = "deActivateVicketGroupUser_ERROR_groupUserAlreadyCancelled";
    public static final String deActivateVicketGroupUser_ERROR_params = "deActivateVicketGroupUser_ERROR_params";
    public static final String deActivateVicketGroupUser_ERROR_userWithoutPrivilege = "deActivateVicketGroupUser_ERROR_userWithoutPrivilege";
    public static final String deActivateVicketGroupUser_ERROR_groupUserNotPending = "deActivateVicketGroupUser_ERROR_groupUserNotPending";
    public static final String deActivateVicketGroupUser_ERROR_groupNotFound = "deActivateVicketGroupUser_ERROR_groupNotFound";

    public static final String activateVicketGroupUser_OK = "activateVicketGroupUser_OK_subscription_";
    public static final String activateVicketGroupUser_ERROR_params = "activateVicketGroupUser_ERROR_params";
    public static final String activateVicketGroupUser_ERROR_userWithoutPrivilege = "activateVicketGroupUser_ERROR_userWithoutPrivilege";
    public static final String activateVicketGroupUser_ERROR_groupUserNotPending = "activateVicketGroupUser_ERROR_groupUserNotPending";
    public static final String activateVicketGroupUser_ERROR_groupNotFound = "activateVicketGroupUser_ERROR_groupNotFound";



    public static final String saveVicketGroup_EXCEPTION = "saveVicketGroup_EXCEPTION";
    public static final String saveVicketGroup_ERROR_nameGroupRepeatedMsg = "saveVicketGroup_ERROR_nameGroupRepeatedMsg";
    public static final String saveVicketGroup_ERROR_params = "saveVicketGroup_ERROR_params";
    public static final String saveVicketGroup_OK = "saveVicketGroup_OK_groupvs_id_";


    public static final String subscribeToVicketGroup_EXCEPTION = "subscribeToVicketGroup_EXCEPTION";
    public static final String subscribeToVicketGroup_ERROR_userAlreadySubscribed = "subscribeToVicketGroup_ERROR_userAlreadySubscribed";
    public static final String subscribeToVicketGroup_ERROR_representativeSubscribed = "subscribeToVicketGroup_ERROR_representativeSubscribed";
    public static final String subscribeToVicketGroup_ERROR_params = "subscribeToVicketGroup_ERROR_params";
    public static final String subscribeToVicketGroup_OK = "subscribeToVicketGroup_OK_";

    public static final String processVicketDeposit_ERROR_IBAN_code = "processVicketDeposit_ERROR_IBAN_code";
    public static final String processDeposit_ERROR_IBAN_code = "processDeposit_ERROR_IBAN_code";

}
