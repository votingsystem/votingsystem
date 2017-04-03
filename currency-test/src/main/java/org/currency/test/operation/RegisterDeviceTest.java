package org.currency.test.operation;

import org.currency.test.Constants;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.OperationType;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RegisterDeviceTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RegisterDeviceTest.class.getName());

    public static final String REQUEST = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B\n" +
            "BwGggCSABIID6HsiZGV2aWNlSWQiOiIzN2E0NzUxZi04NjIyLTRlZDgtYmZjNC0z\n" +
            "YmIwZDdlYzMwNWMiLCJtb2JpbGVDc3IiOiItLS0tLUJFR0lOIENFUlRJRklDQVRF\n" +
            "IFJFUVVFU1QtLS0tLVxuTUlJRFJqQ0NBaTRDQVFBd096RVBNQTBHQTFVRUJCTUdS\n" +
            "MEZTUTBsQk1SUXdFZ1lEVlFRcUV3dEtUMU5GSUVwQlxuVmtsRlVqRVNNQkFHQTFV\n" +
            "RUJSTUpNRGMxTlRNeE56SklNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4\n" +
            "QVxuTUlJQkNnS0NBUUVBMnEvQ05ncngrRHowdm5pRjY4akRid0NFR2tsbmR4VDh0\n" +
            "YmRabjY3czFqZUF3WGJZeUpISlxucTlralBzdHFWVXlnbXlZTG5mcHUzVlBnRHlQ\n" +
            "VEd4R0k1RlJnWWpWSWlmUHR2UGVJNEx1STVRSGlyNXVoZ1l3MVxubnR4N20xRFdR\n" +
            "dHZKTzNDTXpmRWI0VHN6ZmQ5dXRwb1dFR0Q3bkYxNjl2amxlbElRM25aTG9WdG1Q\n" +
            "VlRMSnVkVVxudDAwMnlSWW9mb29IenJ5VVVqUUsxdHordXMwWllUOTc4SUNSclky\n" +
            "TVJJUS9lK3lNR3djTW10bTBBODJKcHBwNlxuUlU1N1ppbTh6bTNhRW9KQmRibmdT\n" +
            "aXlvZzhUeW5FTW1WUzVPbGtJeXhHMFl6Uk1wSWtBaENIRWo1b3d2bkQzWFxuVTdE\n" +
            "Z2RPVnR1N0JhWGFSQzY0djRVQ0RnUFZGRzRhdnBWUUlEQVFBQm9JSEZNSUhDQmdr\n" +
            "QUFBQUFBQUFBQUFReFxuZ2JRTWdiRjdJbVJsZG1salpVNWhiV1VpT2lKamRYSnla\n" +
            "VzVqZVMxdGIySnBiR1V0WVhCd2JHbGpZWFJwYjI0aVxuTENKa1pYWnBZMlZVZVhC\n" +
            "bElqb2lUVTlDU1V4Rklpd2laMmwyWlc1dVlXMWxJam9pU2s5VFJTQktRVlpKUlZJ\n" +
            "aVxuTENKdWRXMUpaQ0k2SWpBM05UVXpNVGN5U0NJc0luTjFjbTVoYldVaU9pSkhR\n" +
            "VkpEU1VFaUxDSjFkV2xrSWpvaVxuTXpkaE5EYzFNV1l0T0RZeU1pMDBaV1E0TFdK\n" +
            "bVl6UXRNMkppTUdRM1pXTXpNRFZqSW4wd0RRWUpLb1pJaHZjTlxuQVFFTEJRQURn\n" +
            "Z0VCQUxDUFhIRVdIOW9tQXZXblZWa1VVdkVwSTVpS1hCZEJXejBmbDcweEF5VDcx\n" +
            "a1pBRm1Ha1xucnFlSk1VMDkzNHRua2N0a21MTENOSkx2T3lpem5MRENNRVUzbXc5\n" +
            "Y1UEggIrbklSRlhac0ZoTUlPYlI3anNyTmZQbWJcbkNHdWVPRDVmVjdYcE5oSjd6\n" +
            "Rk1yTW9ZdmVPOFBITlJkZ0o2RWxFOEhyTHRobmRoQXBXb3Q4ZmphL2daRjFLWkFc\n" +
            "bjNrY0h0enVpWk9LL1VVYWJCeC9LT2Uyam5FdzRnZXhzaW04Y2x3aVBIMDJNV2dM\n" +
            "eFdmamhlVzZOdFFPVUFOMEtcbnBCUnZVMzV0aUZlRnpIOVVFTUlGYkJHZ2c0djVz\n" +
            "SjJsMldwMFNjTGZ0OERVMS9HMUdqcTcrYWw1MDc0WW5xbFRcblhDQmV4MElzK2U0\n" +
            "SzdkQnpCN3JuQlVKS1JRREl2VFUxQmRzPVxuLS0tLS1FTkQgQ0VSVElGSUNBVEUg\n" +
            "UkVRVUVTVC0tLS0tXG4iLCJvcGVyYXRpb24iOnsiRW50aXR5SWQiOiJodHRwczov\n" +
            "L3ZvdGluZy5kZG5zLm5ldC9jdXJyZW5jeS1zZXJ2ZXIiLCJUeXBlIjoiUkVHSVNU\n" +
            "RVIifSwidXNlciI6eyJmdWxsTmFtZSI6IkpPU0UgSkFWSUVSIEdBUkNJQSIsIkNv\n" +
            "dW50cnkiOiJHQVJDSUEgWk9STk9aQSwgSk9TRSBKQVZJRVIgKEZJUk1BKSIsIkNl\n" +
            "cnRpZmljYXRlcyI6W10sIkdpdmVuTmFtZSI6IkpPU0UgSkFWSUVSIiwiTnVtSWQi\n" +
            "OiIwNzU1MzE3MkgiLCJTdXJOYW1lIjoiR0FSQ0lBIn19AAAAAAAAoIAwggXkMIIE\n" +
            "zKADAgECAhBYA922/u6bI1YSIINg0YmfMA0GCSqGSIb3DQEBBQUAMFwxCzAJBgNV\n" +
            "BAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04gR0VORVJBTCBERSBMQSBQT0xJQ0lB\n" +
            "MQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtBQyBETklFIDAwMTAeFw0xNTEwMDUw\n" +
            "NzAyMjdaFw0yMDEwMDUwNzAyMjZaMHYxCzAJBgNVBAYTAkVTMRIwEAYDVQQFEwkw\n" +
            "NzU1MzE3MkgxDzANBgNVBAQMBkdBUkNJQTEUMBIGA1UEKgwLSk9TRSBKQVZJRVIx\n" +
            "LDAqBgNVBAMMI0dBUkNJQSBaT1JOT1pBLCBKT1NFIEpBVklFUiAoRklSTUEpMIIB\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmYQML+ybhr3DWiZEnFFQ0XGS\n" +
            "JuiTp3Z4itHPou1S8Ib7WD+pq6FMz+4krUo5fvK7me9i5v1JQOZDP/0GgRTyOvP3\n" +
            "RCH6DqD3YWWydW0XvkcBjLXTd8LGJC3pc6lFQw8YI8b5pJOzvyyTgN+BFEwDtsxu\n" +
            "WPgLDF0JNKKPzHmYT93+647sTzH5WRpi/BcDJ6dXzKQYxHU4nTAYKXXcn49IzYuZ\n" +
            "+SVXub14oEVCS8IvgMPvHZTfyrhcSfjAfLdXkG4fesDWIjM/UTFxcBs1X5NcryVm\n" +
            "/DLIFoZgfbuhKlH6I4rJXZJ4rzbCA8pmMSSbhZvviYgXG6NiLVQXUHH3XTcTfwID\n" +
            "AQABo4IChjCCAoIwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUxXzBlpUmg2yi2ERb\n" +
            "1umedGWxZ/wwHwYDVR0jBBgwFoAUGomoxe6Pdl1VcYnzOzW9qgUAlW8wIgYIKwYB\n" +
            "BQUHAQMEFjAUMAgGBgQAjkYBATAIBgYEAI5GAQQwYAYIKwYBBQUHAQEEVDBSMB8G\n" +
            "CCsGAQUFBzABhhNodHRwOi8vb2NzcC5kbmllLmVzMC8GCCsGAQUFBzAChiNodHRw\n" +
            "Oi8vd3d3LmRuaWUuZXMvY2VydHMvQUNSYWl6LmNydDA7BgNVHSAENDAyMDAGCGCF\n" +
            "VAECAgIDMCQwIgYIKwYBBQUHAgEWFmh0dHA6Ly93d3cuZG5pZS5lcy9kcGMwgfAG\n" +
            "CCsGAQUFBwECBIHjMIHgMDICAQEwCwYJYIZIAWUDBAIBBCCMUhL0noDiDEfrQoYP\n" +
            "M8DOM3MFRo1yx3SgwpLpS1nKOTAyAgEAMAsGCWCGSAFlAwQCAQQgq1sGgTN8939C\n" +
            "DY/rewqP/7UouRGkKA7YQzPy2j9Y3ywwOgYJYIVUAQICBAIBMAsGCWCGSAFlAwQC\n" +
            "AQQgjOiPE6LqUZUSFkVJBCwtfdEaFeBZ8ycJkwjKpQRIq7kwOgYJYIVUAQICBAIG\n" +
            "MAsGCWCGSAFlAwQCAQQg6TKVaKPzXujYvuC6P2LS001rPByb06tzAMxNjUmT/30w\n" +
            "KAYDVR0JBCEwHzAdBggrBgEFBQcJATERGA8xOTcxMDMxODEyMDAwMFowQgYIYIVU\n" +
            "AQICBAEENjA0MDICAQIwCwYJYIZIAWUDBAIBBCCY9hv/YymnkJatfisKwE96KAoU\n" +
            "6DHAxcCSPLTCm8I8DTAOBgNVHQ8BAf8EBAMCBkAwDQYJKoZIhvcNAQEFBQADggEB\n" +
            "AHUQo/BmKgwJRZ770vso3KyKmFlCd4Hv3czc0DqdXzkquXbqeyxOR4JQ+k8KJNqz\n" +
            "I3/G7peZ2VjP0EtHDepp6zelqwlgQn4kX/GWC7GsM0KLJyg3my+BXxJYGxUC3iKd\n" +
            "NwngmOvREoPXK87wgNrWnSfeoxE2pJW+YkWP+nPP1uqr0dJtJLpHYsUpYCm//dU5\n" +
            "BbbyM11pJD1z8fMImS2MCOjsTG6hGYSwrln6S8timztAWzkNPmWgznuVRqcUcDCZ\n" +
            "cEDcrzyPKYePEiuEY7xYI1tmmIf46X2WElbaLwl0T+YzxKb5K74IK6nD7iDUdfcA\n" +
            "D9WKWRd6WvWD+ZSYmqk03FIwggXFMIIDraADAgECAhBkIGbJmXuu4UQC2m6kItZJ\n" +
            "MA0GCSqGSIb3DQEBBQUAMF0xCzAJBgNVBAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJ\n" +
            "T04gR0VORVJBTCBERSBMQSBQT0xJQ0lBMQ0wCwYDVQQLDARETklFMRUwEwYDVQQD\n" +
            "DAxBQyBSQUlaIEROSUUwHhcNMDYwMjI3MTA1NDM4WhcNMjEwMjI2MjI1OTU5WjBc\n" +
            "MQswCQYDVQQGEwJFUzEoMCYGA1UECgwfRElSRUNDSU9OIEdFTkVSQUwgREUgTEEg\n" +
            "UE9MSUNJQTENMAsGA1UECwwERE5JRTEUMBIGA1UEAwwLQUMgRE5JRSAwMDEwggEi\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCs/kqZ0uSn/Y0P7nuMudf7iTV/\n" +
            "zpnfpMau/Lcy4A1X5+3w1s07FlylwZI/DpZhr6bTMp+FD6xARMeEwcyjsZvqeWGo\n" +
            "KY1OlN2PfkrQGfmZoudMTwXz+n/qlP0IusQ9hXl5fs5s0KKqTa+e5ZAxxUDBFfTv\n" +
            "tBgYo3Nb3yn0rGKriZhlFHQ5ufFg13rVYNzM524/kL91QisaTcl8Usg0sZXR3/0R\n" +
            "p4eXvOOTM5jIJJwar62WuQpWBboFDAj+BfDhdFg2EWMIA/Nvxxot6/FDEafNFRnx\n" +
            "1M5Q56qA5nUVd826KDsqh6TiqR3JAqjBg2nhLc3B6669uxs8lmpXMKrPAKr3AgMB\n" +
            "AAGjggGAMIIBfDASBgNVHRMBAf8ECDAGAQH/AgEAMB0GA1UdDgQWBBQaiajF7o92\n" +
            "XVVxifM7Nb2qBQCVbzAfBgNVHSMEGDAWgBSORfSfc8X/LxsF2wFHYBsDioG3ujAO\n" +
            "BgNVHQ8BAf8EBAMCAQYwNwYDVR0gBDAwLjAsBgRVHSAAMCQwIgYIKwYBBQUHAgEW\n" +
            "Fmh0dHA6Ly93d3cuZG5pZS5lcy9kcGMwgdwGA1UdHwSB1DCB0TCBzqCBy6CByIYg\n" +
            "aHR0cDovL2NybHMuZG5pZS5lcy9jcmxzL0FSTC5jcmyGgaNsZGFwOi8vbGRhcC5k\n" +
            "bmllLmVzL0NOPUNSTCxDTj1BQyUyMFJBSVolMjBETklFLE9VPUROSUUsTz1ESVJF\n" +
            "Q0NJT04lMjBHRU5FUkFMJTIwREUlMjBMQSUyMFBPTElDSUEsQz1FUz9hdXRob3Jp\n" +
            "dHlSZXZvY2F0aW9uTGlzdD9iYXNlP29iamVjdGNsYXNzPWNSTERpc3RyaWJ1dGlv\n" +
            "blBvaW50MA0GCSqGSIb3DQEBBQUAA4ICAQBnWP9rsh86Zwn+B6si5RicESToduXT\n" +
            "mmNJO1o5/e3wLFLn3jjSJ50YN3Zu4BsdWhSUrd3hDye/SqyzRcyfoK2jvkIWfYSH\n" +
            "LFl6Tp4ONuYFRwrtU8RaSMx1BlSMMjFpriZ9vEwYD4HHZJ9/TONx2y7EScPNheav\n" +
            "2WU/t+pfzz3sxFqgo/kqTthpy7j2Ou09RhMK0Ae6dkPK2wZI4u3xOEUNqLyQizwd\n" +
            "/x2Im8FQaPL0WVJ6ip+K4C+B08m1Wc153G0R6jn+gZzf70vTY8aqjig+DG0UJwnY\n" +
            "6Wwy+HmPF/xZb+hySdW6k215sqrTvZDvagsgLL29pEK3nEZP6VTShFhdP43jV54d\n" +
            "hblIcxJWrNtza4niljEwWWNPHmI/Q2wqw7A3qKv87Kcewc1sL2CWPXQ+JJCQx1Ty\n" +
            "oPKBt//g4VjDP/hGjOFoxTMPZhe2I6kXxTlHJ6IQcNnSUKQnqka3twYs+EvLOph3\n" +
            "yLNhO1RVc2YkauPnh/UAbhQqLH0jDxfPVbICNtK6PxmfzgU/IiuyAi2V+S0ZKAzE\n" +
            "XXzltrPFC0fKi/l4GiyL0f48GX4I95Dbc7Mm0IrgWyx2mU90wEB4doYifEtmm+3W\n" +
            "qMmc0izRBpcrIePo9yg3UwOBijnJYCPVk8XhKHGJTpMEcf1LHC202bGQoWQ1gXBP\n" +
            "ecqr2PropHcLZwAAMYII2jCCCNYCAQEwcDBcMQswCQYDVQQGEwJFUzEoMCYGA1UE\n" +
            "CgwfRElSRUNDSU9OIEdFTkVSQUwgREUgTEEgUE9MSUNJQTENMAsGA1UECwwERE5J\n" +
            "RTEUMBIGA1UEAwwLQUMgRE5JRSAwMDECEFgD3bb+7psjVhIgg2DRiZ8wDQYJYIZI\n" +
            "AWUDBAIBBQCgggc7MBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcN\n" +
            "AQkFMQ8XDTE3MDQwMTE1NDA0OVowLwYJKoZIhvcNAQkEMSIEIEY7Go404QpggSO0\n" +
            "B1fy31ihLJ8/XvQe0d22HrBrjtR+MIIGzgYLKoZIhvcNAQkQAg4xgga9MIIGuQYJ\n" +
            "KoZIhvcNAQcCoIIGqjCCBqYCAQMxDzANBglghkgBZQMEAgEFADB5BgsqhkiG9w0B\n" +
            "CRABBKBqBGgwZgIBAQYGghKEN4Z6MDEwDQYJYIZIAWUDBAIBBQAEIEY7Go404Qpg\n" +
            "gSO0B1fy31ihLJ8/XvQe0d22HrBrjtR+AghMxMCa9dgQrRgPMjAxNzA0MDExMzQw\n" +
            "NDBaMAsCAQGAAgH0gQIB9KCCA9YwggPSMIICuqADAgECAgYBWxrxWjIwDQYJKoZI\n" +
            "hvcNAQELBQAwLDEaMBgGA1UEAwwRRkFLRSBST09UIEROSWUgQ0ExDjAMBgNVBAsM\n" +
            "BUNlcnRzMB4XDTE3MDMyOTE0NDExNloXDTE4MDMyOTE0NDExNlowKzEpMCcGA1UE\n" +
            "KhMgVm90aW5nLUN1cnJlbmN5IFRpbWVzdGFtcCBTZXJ2ZXIwggEiMA0GCSqGSIb3\n" +
            "DQEBAQUAA4IBDwAwggEKAoIBAQCCpJ/auG6kGBZv8DO80212qKm4xKHjKzuwAoMQ\n" +
            "CUXE2CoxhTlCXx946EP258TcgLplfl/CrpDPVWw1tdHYGCcoioiirIe9Ty4mlPH7\n" +
            "+1sj+7hyeEnMCei6PppRZLs2KNWZZhsFCukbEPFXUNiTDKVN6BaxmtVgN6+HCKSo\n" +
            "XVB1PprminS83WbAW5g3hLP8IcDI3sjIiaLkBmNSzt0z3R1AzQqZBfxx6XzkUpD3\n" +
            "k4o7HaJaVh89SfosNRBFuUTh9YdkXKkpm1qT/2SacN3tBTIhAgI0A4Wl84oXr0Um\n" +
            "fLGKMEbY1M//syEASb3eT4hjBmY4MJWgXxp9ywA9SL0vQsC3AgMBAAGjgfowgfcw\n" +
            "QwYIKwYBBQUHAQEENzA1MDMGCCsGAQUFBzABhidodHRwczovL3ZvdGluZy5kZG5z\n" +
            "Lm5ldC9pZHByb3ZpZGVyL29jc3AwWwYDVR0jBFQwUoAUPZjbh7NDC2DDAzA9Ymqy\n" +
            "a9C8XDChMKQuMCwxGjAYBgNVBAMMEUZBS0UgUk9PVCBETkllIENBMQ4wDAYDVQQL\n" +
            "DAVDZXJ0c4IIb8ww+Lwg1OowHQYDVR0OBBYEFKFt/Rd73wAuPUvaek1w7VIJgKyC\n" +
            "MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgWgMBYGA1UdJQEB/wQMMAoGCCsG\n" +
            "AQUFBwMIMA0GCSqGSIb3DQEBCwUAA4IBAQCdxSP35W8yfaW1m5wi8MuyYghtDzq3\n" +
            "6maP7OI1ZI9FoxBBBn9NrsXhpibY+0g4jmDxSFL59PedzXileTujbCgOdCQb/p8r\n" +
            "YjAVDcp+grZoNTrOiEcbHsE9g+6zNiPwr+keOPODkyzXth8vjkMi7+0OCPo7RoP2\n" +
            "2MWwB+wvXdoVh8HIM0t5rc+DMr2cEzES0wwjGcKaCJKQ0Z34pwnF9GrAz0kUX7FR\n" +
            "VtoP7u3WV0JCZg/anK2sR+1rRa0ZQ+EVRZ5a/ZYYneuW1hlI2LUer+TrEmLsY9Yj\n" +
            "GqFXkoAw4GOLJ20GnKrr/J1klvgA0iG4SNGzADJtWWOWtBwwFCaMXJlGMYICOTCC\n" +
            "AjUCAQEwNjAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5JZSBDQTEOMAwGA1UECwwF\n" +
            "Q2VydHMCBgFbGvFaMjANBglghkgBZQMEAgEFAKCB0zAaBgkqhkiG9w0BCQMxDQYL\n" +
            "KoZIhvcNAQkQAQQwHAYJKoZIhvcNAQkFMQ8XDTE3MDQwMTEzNDA0MFowLQYJKoZI\n" +
            "hvcNAQk0MSAwHjANBglghkgBZQMEAgEFAKENBgkqhkiG9w0BAQEFADAvBgkqhkiG\n" +
            "9w0BCQQxIgQgOKCQXTgBD3MtlqduzPEQm/cauqfhX0N1n69XljRd3tIwNwYLKoZI\n" +
            "hvcNAQkQAi8xKDAmMCQwIgQgOmjltzTsTjgbUwlOPP2FPYH4sHLn8BMyjB1twm9S\n" +
            "UBQwDQYJKoZIhvcNAQEBBQAEggEAa/Aq3Y2ZD1o3wK3A/LpwF+iuW9If1JNq9Rr3\n" +
            "dqw4MHTfPWAiNx9tcZRCXRoPyDUeA5UfQrT7PbcbXlbTHZClOcsQQdj4VHH/B3xx\n" +
            "JRk9VaEqyNrn00bs5yRZZJtv7mb9kDwXUwKrexQIFb7gxT27PKuFNobwzCRjHmC6\n" +
            "p3bEP2Dw3rVt9YMINObmDg8BPP2lmuCDjInDR9Z/uo/g7W270D97NwX+G5YjtgBf\n" +
            "DNbMl/Ct8QGmo/6eqJFRB1xMa6PpL/eFxXeRwDsUOauB286iDH01ft3T8148Ruih\n" +
            "Wp0qcSlN5zqNaFarCWPG0XgXIXGeahlZO4aw6DBZIKzrQPHgDKEAMA0GCSqGSIb3\n" +
            "DQEBAQUABIIBABwGwXQ8hkIGn6/nH0T4CTwD+Dz2BNwXtqn8H4zI/9bilz7BWULX\n" +
            "sTMGk18+yJHFU5dP5WdjGMrNcmmtyS12FfM75LW/g+T6klKiFUBMe1XJ9lOti871\n" +
            "3/M88yP8nf5y3QtUCxi9K/lv52rs5Jpyzg5N2GVsB9UtoKg8J8DFx/eBWdBWYtIZ\n" +
            "vUDUQc1gx/5E+rYvSiTHvGv8GrJUuceBYhKhfjcKINdfd7N/HUuYiOXTDmzPLzbK\n" +
            "vGbR53TOI09Lytf+3Gl0mxmYcx3H/Eds4L1Rscr/B+o+VFIpGFkSpDDufPNwbeUr\n" +
            "SHn8fqbur4d7rptuJf+NXNxog0++bXFaEWIAAAAAAAA=\n" +
            "-----END PKCS7-----";
    public static void main(String[] args) throws Exception {
        new RegisterDeviceTest().checkRequest();
        System.exit(0);
    }

    public void checkRequest() throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(REQUEST);
        RegisterDto registerDto = cmsSignedMessage.getSignedContent(RegisterDto.class);
        log.info("SignedContent: " + cmsSignedMessage.getSignedContentStr());
        log.info("DeviceId: " + registerDto.getDeviceId());
    }

    public void registerDevice() {
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(REQUEST.getBytes(), MediaType.PKCS7_SIGNED,
                OperationType.REGISTER.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("status: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }

}