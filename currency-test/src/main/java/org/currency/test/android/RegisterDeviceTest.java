package org.currency.test.android;

import org.currency.test.Constants;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;

import java.security.cert.X509Certificate;
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
            "QVxuTUlJQkNnS0NBUUVBM3d0ZCs3ZWN1R2ppSEhsektpeFJBR1ArdUcrczR3WmJz\n" +
            "YzRldDZjdXN0RHo1eDZ2YnAyd1xuODhmVHZlVDJyQ3FBejdjV2IwWlVIZlJxcmpK\n" +
            "S1hlTXlrdmtjMmp4TGNpazc2VGZjdmZXdFc2NWhhM0NOcWMvSlxuK3BUbnAvdjBS\n" +
            "bnBaN3ZKdkhMOTkvclB3ZDZOb2VkeFcrS3lWZkkwWlFJeGVESnZEbUJpYlZOWXBD\n" +
            "dTN1VzhmSlxuam5sSmVHK1B3VUNnc2crY2dOelh4UmxXNVFEcHJubnlGejltRnNm\n" +
            "REhzSVpVb2Z1QzEzVWlLOTNlZVVBbzQyalxuTDUxMzB5TVFUUWlXRmk3R1V5RXZB\n" +
            "THJISTJkSHNrMlErVXBZTkowOW9VWFE3QWdYUGxMSDVHOU1nYldyYVB0OFxuU1VS\n" +
            "SFVnak5TbWZpbThjc0liamw1elhpM0xPUkRYcnBkUUlEQVFBQm9JSEZNSUhDQmdr\n" +
            "QUFBQUFBQUFBQUFReFxuZ2JRTWdiRjdJbVJsZG1salpVNWhiV1VpT2lKamRYSnla\n" +
            "VzVqZVMxdGIySnBiR1V0WVhCd2JHbGpZWFJwYjI0aVxuTENKa1pYWnBZMlZVZVhC\n" +
            "bElqb2lUVTlDU1V4Rklpd2laMmwyWlc1dVlXMWxJam9pU2s5VFJTQktRVlpKUlZJ\n" +
            "aVxuTENKdWRXMUpaQ0k2SWpBM05UVXpNVGN5U0NJc0luTjFjbTVoYldVaU9pSkhR\n" +
            "VkpEU1VFaUxDSjFkV2xrSWpvaVxuTXpkaE5EYzFNV1l0T0RZeU1pMDBaV1E0TFdK\n" +
            "bVl6UXRNMkppTUdRM1pXTXpNRFZqSW4wd0RRWUpLb1pJaHZjTlxuQVFFTEJRQURn\n" +
            "Z0VCQUFNMFlQcDBRY3lwNythY083SDViRllQS1dQUjhXUnFMa0FBNmhzYm9XaEtj\n" +
            "RXhFamJvZFxuZm5ocTFCWW40S0M2SWpXMHNac2JlRll1S0gzenBRbHZtRTNFcWtF\n" +
            "OFQEggJpN2Z0RERmaGZncHA1VjdjelV2TVRjMU9cbmFFREtEVzNnT1doQWZMcC9S\n" +
            "QXlKbnhFZlBKSFlOTHlvV1FOWHJ2TExvWG5FOXlsYzY5U3Z4WUpmMDdhSHp3Yi9c\n" +
            "blNIWldvMU1rWVc1WGhYV3pTWlpwN0IzaWtzdGZOS2UwYTc3WHNTTnBoSUl3Tm10\n" +
            "WDlzYXovOG9xdmhGeVBpNGNcbmZkQ0VsQ3NZSmNKSFJtMUxGVCt5WDdOTUVhTTNT\n" +
            "V0pLSTlSaUxuY000VmdwYU9EV0hTNnJtTzZIcjk2M1hRZU1cbk1BektKT0V3L05p\n" +
            "VnFOWWp2eCtqdU52TzVSVDNoNmtyUDN3PVxuLS0tLS1FTkQgQ0VSVElGSUNBVEUg\n" +
            "UkVRVUVTVC0tLS0tXG4iLCJ1c2VyIjp7ImZ1bGxOYW1lIjoiSk9TRSBKQVZJRVIg\n" +
            "R0FSQ0lBIiwiQ291bnRyeSI6IkdBUkNJQSBaT1JOT1pBLCBKT1NFIEpBVklFUiAo\n" +
            "RklSTUEpIiwiQ2VydGlmaWNhdGVzIjpbeyJyb290IjpmYWxzZX1dLCJHaXZlbk5h\n" +
            "bWUiOiJKT1NFIEpBVklFUiIsIk51bUlkIjoiMDc1NTMxNzJIIiwiU3VyTmFtZSI6\n" +
            "IkdBUkNJQSJ9LCJ1dWlkIjoiZWU4N2QxNTItYTVhMi00YTFhLWI0ODItOTdjZWU3\n" +
            "ZGU1NDhkIiwiT3BlcmF0aW9uIjp7IkVudGl0eUlkIjoiaHR0cHM6Ly92b3Rpbmcu\n" +
            "ZGRucy5uZXQvaWRwcm92aWRlciIsIlR5cGUiOiJSRUdJU1RFUl9ERVZJQ0UifX0A\n" +
            "AAAAAACggDCCBeQwggTMoAMCAQICEFgD3bb+7psjVhIgg2DRiZ8wDQYJKoZIhvcN\n" +
            "AQEFBQAwXDELMAkGA1UEBhMCRVMxKDAmBgNVBAoMH0RJUkVDQ0lPTiBHRU5FUkFM\n" +
            "IERFIExBIFBPTElDSUExDTALBgNVBAsMBEROSUUxFDASBgNVBAMMC0FDIEROSUUg\n" +
            "MDAxMB4XDTE1MTAwNTA3MDIyN1oXDTIwMTAwNTA3MDIyNlowdjELMAkGA1UEBhMC\n" +
            "RVMxEjAQBgNVBAUTCTA3NTUzMTcySDEPMA0GA1UEBAwGR0FSQ0lBMRQwEgYDVQQq\n" +
            "DAtKT1NFIEpBVklFUjEsMCoGA1UEAwwjR0FSQ0lBIFpPUk5PWkEsIEpPU0UgSkFW\n" +
            "SUVSIChGSVJNQSkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCZhAwv\n" +
            "7JuGvcNaJkScUVDRcZIm6JOndniK0c+i7VLwhvtYP6mroUzP7iStSjl+8ruZ72Lm\n" +
            "/UlA5kM//QaBFPI68/dEIfoOoPdhZbJ1bRe+RwGMtdN3wsYkLelzqUVDDxgjxvmk\n" +
            "k7O/LJOA34EUTAO2zG5Y+AsMXQk0oo/MeZhP3f7rjuxPMflZGmL8FwMnp1fMpBjE\n" +
            "dTidMBgpddyfj0jNi5n5JVe5vXigRUJLwi+Aw+8dlN/KuFxJ+MB8t1eQbh96wNYi\n" +
            "Mz9RMXFwGzVfk1yvJWb8MsgWhmB9u6EqUfojisldknivNsIDymYxJJuFm++JiBcb\n" +
            "o2ItVBdQcfddNxN/AgMBAAGjggKGMIICgjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n" +
            "BBTFfMGWlSaDbKLYRFvW6Z50ZbFn/DAfBgNVHSMEGDAWgBQaiajF7o92XVVxifM7\n" +
            "Nb2qBQCVbzAiBggrBgEFBQcBAwQWMBQwCAYGBACORgEBMAgGBgQAjkYBBDBgBggr\n" +
            "BgEFBQcBAQRUMFIwHwYIKwYBBQUHMAGGE2h0dHA6Ly9vY3NwLmRuaWUuZXMwLwYI\n" +
            "KwYBBQUHMAKGI2h0dHA6Ly93d3cuZG5pZS5lcy9jZXJ0cy9BQ1JhaXouY3J0MDsG\n" +
            "A1UdIAQ0MDIwMAYIYIVUAQICAgMwJDAiBggrBgEFBQcCARYWaHR0cDovL3d3dy5k\n" +
            "bmllLmVzL2RwYzCB8AYIKwYBBQUHAQIEgeMwgeAwMgIBATALBglghkgBZQMEAgEE\n" +
            "IIxSEvSegOIMR+tChg8zwM4zcwVGjXLHdKDCkulLWco5MDICAQAwCwYJYIZIAWUD\n" +
            "BAIBBCCrWwaBM3z3f0INj+t7Co//tSi5EaQoDthDM/LaP1jfLDA6BglghVQBAgIE\n" +
            "AgEwCwYJYIZIAWUDBAIBBCCM6I8ToupRlRIWRUkELC190RoV4FnzJwmTCMqlBEir\n" +
            "uTA6BglghVQBAgIEAgYwCwYJYIZIAWUDBAIBBCDpMpVoo/Ne6Ni+4Lo/YtLTTWs8\n" +
            "HJvTq3MAzE2NSZP/fTAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5NzEwMzE4\n" +
            "MTIwMDAwWjBCBghghVQBAgIEAQQ2MDQwMgIBAjALBglghkgBZQMEAgEEIJj2G/9j\n" +
            "KaeQlq1+KwrAT3ooChToMcDFwJI8tMKbwjwNMA4GA1UdDwEB/wQEAwIGQDANBgkq\n" +
            "hkiG9w0BAQUFAAOCAQEAdRCj8GYqDAlFnvvS+yjcrIqYWUJ3ge/dzNzQOp1fOSq5\n" +
            "dup7LE5HglD6Twok2rMjf8bul5nZWM/QS0cN6mnrN6WrCWBCfiRf8ZYLsawzQosn\n" +
            "KDebL4FfElgbFQLeIp03CeCY69ESg9crzvCA2tadJ96jETaklb5iRY/6c8/W6qvR\n" +
            "0m0kukdixSlgKb/91TkFtvIzXWkkPXPx8wiZLYwI6OxMbqEZhLCuWfpLy2KbO0Bb\n" +
            "OQ0+ZaDOe5VGpxRwMJlwQNyvPI8ph48SK4RjvFgjW2aYh/jpfZYSVtovCXRP5jPE\n" +
            "pvkrvggrqcPuINR19wAP1YpZF3pa9YP5lJiaqTTcUjCCBcUwggOtoAMCAQICEGQg\n" +
            "ZsmZe67hRALabqQi1kkwDQYJKoZIhvcNAQEFBQAwXTELMAkGA1UEBhMCRVMxKDAm\n" +
            "BgNVBAoMH0RJUkVDQ0lPTiBHRU5FUkFMIERFIExBIFBPTElDSUExDTALBgNVBAsM\n" +
            "BEROSUUxFTATBgNVBAMMDEFDIFJBSVogRE5JRTAeFw0wNjAyMjcxMDU0MzhaFw0y\n" +
            "MTAyMjYyMjU5NTlaMFwxCzAJBgNVBAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04g\n" +
            "R0VORVJBTCBERSBMQSBQT0xJQ0lBMQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtB\n" +
            "QyBETklFIDAwMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKz+SpnS\n" +
            "5Kf9jQ/ue4y51/uJNX/Omd+kxq78tzLgDVfn7fDWzTsWXKXBkj8OlmGvptMyn4UP\n" +
            "rEBEx4TBzKOxm+p5YagpjU6U3Y9+StAZ+Zmi50xPBfP6f+qU/Qi6xD2FeXl+zmzQ\n" +
            "oqpNr57lkDHFQMEV9O+0GBijc1vfKfSsYquJmGUUdDm58WDXetVg3Mznbj+Qv3VC\n" +
            "KxpNyXxSyDSxldHf/RGnh5e845MzmMgknBqvrZa5ClYFugUMCP4F8OF0WDYRYwgD\n" +
            "82/HGi3r8UMRp80VGfHUzlDnqoDmdRV3zbooOyqHpOKpHckCqMGDaeEtzcHrrr27\n" +
            "GzyWalcwqs8AqvcCAwEAAaOCAYAwggF8MBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYD\n" +
            "VR0OBBYEFBqJqMXuj3ZdVXGJ8zs1vaoFAJVvMB8GA1UdIwQYMBaAFI5F9J9zxf8v\n" +
            "GwXbAUdgGwOKgbe6MA4GA1UdDwEB/wQEAwIBBjA3BgNVHSAEMDAuMCwGBFUdIAAw\n" +
            "JDAiBggrBgEFBQcCARYWaHR0cDovL3d3dy5kbmllLmVzL2RwYzCB3AYDVR0fBIHU\n" +
            "MIHRMIHOoIHLoIHIhiBodHRwOi8vY3Jscy5kbmllLmVzL2NybHMvQVJMLmNybIaB\n" +
            "o2xkYXA6Ly9sZGFwLmRuaWUuZXMvQ049Q1JMLENOPUFDJTIwUkFJWiUyMEROSUUs\n" +
            "T1U9RE5JRSxPPURJUkVDQ0lPTiUyMEdFTkVSQUwlMjBERSUyMExBJTIwUE9MSUNJ\n" +
            "QSxDPUVTP2F1dGhvcml0eVJldm9jYXRpb25MaXN0P2Jhc2U/b2JqZWN0Y2xhc3M9\n" +
            "Y1JMRGlzdHJpYnV0aW9uUG9pbnQwDQYJKoZIhvcNAQEFBQADggIBAGdY/2uyHzpn\n" +
            "Cf4HqyLlGJwRJOh25dOaY0k7Wjn97fAsUufeONInnRg3dm7gGx1aFJSt3eEPJ79K\n" +
            "rLNFzJ+graO+QhZ9hIcsWXpOng425gVHCu1TxFpIzHUGVIwyMWmuJn28TBgPgcdk\n" +
            "n39M43HbLsRJw82F5q/ZZT+36l/PPezEWqCj+SpO2GnLuPY67T1GEwrQB7p2Q8rb\n" +
            "Bkji7fE4RQ2ovJCLPB3/HYibwVBo8vRZUnqKn4rgL4HTybVZzXncbRHqOf6BnN/v\n" +
            "S9NjxqqOKD4MbRQnCdjpbDL4eY8X/Flv6HJJ1bqTbXmyqtO9kO9qCyAsvb2kQrec\n" +
            "Rk/pVNKEWF0/jeNXnh2FuUhzElas23NrieKWMTBZY08eYj9DbCrDsDeoq/zspx7B\n" +
            "zWwvYJY9dD4kkJDHVPKg8oG3/+DhWMM/+EaM4WjFMw9mF7YjqRfFOUcnohBw2dJQ\n" +
            "pCeqRre3Biz4S8s6mHfIs2E7VFVzZiRq4+eH9QBuFCosfSMPF89VsgI20ro/GZ/O\n" +
            "BT8iK7ICLZX5LRkoDMRdfOW2s8ULR8qL+XgaLIvR/jwZfgj3kNtzsybQiuBbLHaZ\n" +
            "T3TAQHh2hiJ8S2ab7daoyZzSLNEGlysh4+j3KDdTA4GKOclgI9WTxeEocYlOkwRx\n" +
            "/UscLbTZsZChZDWBcE95yqvY+uikdwtnAAAxggjaMIII1gIBATBwMFwxCzAJBgNV\n" +
            "BAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04gR0VORVJBTCBERSBMQSBQT0xJQ0lB\n" +
            "MQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtBQyBETklFIDAwMQIQWAPdtv7umyNW\n" +
            "EiCDYNGJnzANBglghkgBZQMEAgEFAKCCBzswGAYJKoZIhvcNAQkDMQsGCSqGSIb3\n" +
            "DQEHATAcBgkqhkiG9w0BCQUxDxcNMTcwNDA1MTEwMjUyWjAvBgkqhkiG9w0BCQQx\n" +
            "IgQgjeI/JICIlx5FcyDXYwFRq5WMkQt83ZGidMHwlsvhrg0wggbOBgsqhkiG9w0B\n" +
            "CRACDjGCBr0wgga5BgkqhkiG9w0BBwKgggaqMIIGpgIBAzEPMA0GCWCGSAFlAwQC\n" +
            "AQUAMHkGCyqGSIb3DQEJEAEEoGoEaDBmAgEBBgaCEoQ3hnowMTANBglghkgBZQME\n" +
            "AgEFAAQgjeI/JICIlx5FcyDXYwFRq5WMkQt83ZGidMHwlsvhrg0CCFwOUD0Ssjxw\n" +
            "GA8yMDE3MDQwNTA5MDI1MlowCwIBAYACAfSBAgH0oIID1jCCA9IwggK6oAMCAQIC\n" +
            "BgFbGvFaMjANBgkqhkiG9w0BAQsFADAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5J\n" +
            "ZSBDQTEOMAwGA1UECwwFQ2VydHMwHhcNMTcwMzI5MTQ0MTE2WhcNMTgwMzI5MTQ0\n" +
            "MTE2WjArMSkwJwYDVQQqEyBWb3RpbmctQ3VycmVuY3kgVGltZXN0YW1wIFNlcnZl\n" +
            "cjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIKkn9q4bqQYFm/wM7zT\n" +
            "bXaoqbjEoeMrO7ACgxAJRcTYKjGFOUJfH3joQ/bnxNyAumV+X8KukM9VbDW10dgY\n" +
            "JyiKiKKsh71PLiaU8fv7WyP7uHJ4ScwJ6Lo+mlFkuzYo1ZlmGwUK6RsQ8VdQ2JMM\n" +
            "pU3oFrGa1WA3r4cIpKhdUHU+muaKdLzdZsBbmDeEs/whwMjeyMiJouQGY1LO3TPd\n" +
            "HUDNCpkF/HHpfORSkPeTijsdolpWHz1J+iw1EEW5ROH1h2RcqSmbWpP/ZJpw3e0F\n" +
            "MiECAjQDhaXzihevRSZ8sYowRtjUz/+zIQBJvd5PiGMGZjgwlaBfGn3LAD1IvS9C\n" +
            "wLcCAwEAAaOB+jCB9zBDBggrBgEFBQcBAQQ3MDUwMwYIKwYBBQUHMAGGJ2h0dHBz\n" +
            "Oi8vdm90aW5nLmRkbnMubmV0L2lkcHJvdmlkZXIvb2NzcDBbBgNVHSMEVDBSgBQ9\n" +
            "mNuHs0MLYMMDMD1iarJr0LxcMKEwpC4wLDEaMBgGA1UEAwwRRkFLRSBST09UIERO\n" +
            "SWUgQ0ExDjAMBgNVBAsMBUNlcnRzgghvzDD4vCDU6jAdBgNVHQ4EFgQUoW39F3vf\n" +
            "AC49S9p6TXDtUgmArIIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBaAwFgYD\n" +
            "VR0lAQH/BAwwCgYIKwYBBQUHAwgwDQYJKoZIhvcNAQELBQADggEBAJ3FI/flbzJ9\n" +
            "pbWbnCLwy7JiCG0POrfqZo/s4jVkj0WjEEEGf02uxeGmJtj7SDiOYPFIUvn0953N\n" +
            "eKV5O6NsKA50JBv+nytiMBUNyn6Ctmg1Os6IRxsewT2D7rM2I/Cv6R4484OTLNe2\n" +
            "Hy+OQyLv7Q4I+jtGg/bYxbAH7C9d2hWHwcgzS3mtz4MyvZwTMRLTDCMZwpoIkpDR\n" +
            "nfinCcX0asDPSRRfsVFW2g/u7dZXQkJmD9qcraxH7WtFrRlD4RVFnlr9lhid65bW\n" +
            "GUjYtR6v5OsSYuxj1iMaoVeSgDDgY4snbQacquv8nWSW+ADSIbhI0bMAMm1ZY5a0\n" +
            "HDAUJoxcmUYxggI5MIICNQIBATA2MCwxGjAYBgNVBAMMEUZBS0UgUk9PVCBETkll\n" +
            "IENBMQ4wDAYDVQQLDAVDZXJ0cwIGAVsa8VoyMA0GCWCGSAFlAwQCAQUAoIHTMBoG\n" +
            "CSqGSIb3DQEJAzENBgsqhkiG9w0BCRABBDAcBgkqhkiG9w0BCQUxDxcNMTcwNDA1\n" +
            "MDkwMjUyWjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQCAQUAoQ0GCSqGSIb3\n" +
            "DQEBAQUAMC8GCSqGSIb3DQEJBDEiBCDhpo7oDaCIONNT7WuOjueqhmajg15XTJ2w\n" +
            "pzHnsU1yQzA3BgsqhkiG9w0BCRACLzEoMCYwJDAiBCA6aOW3NOxOOBtTCU48/YU9\n" +
            "gfiwcufwEzKMHW3Cb1JQFDANBgkqhkiG9w0BAQEFAASCAQAq7e5h4dppVprKjWXY\n" +
            "HWQW2Dq+CZ7QJDyH6WTIvWTGF1sne/rojGb+5XjCKXQxc0OBEIXCuouxvP4OZYW8\n" +
            "NB9QZENeE0nr0UUekC+knjx9raQMBd5ukMRNwf1v1eJYbkKE0exDg2O16uRvXHzA\n" +
            "6YlhobD+kxVfqwVa+2tPYJKN/rh1Uv/9z8N0Jfpg46Ve4KALaXmj+LSJSO7SowGw\n" +
            "2e/7XF0bm7kOyWjMsu4HdacixZTl2Si3SAGErj7FxRPkKU126DGA7F5G5PAN/ffo\n" +
            "qcUkkUPkng+aSfQNKfYzRTQbWMNYwpamSmA7JNLG6mbBcyJlhAGRRMuPv/qKj3Bk\n" +
            "KpMooQAwDQYJKoZIhvcNAQEBBQAEggEAMNtNSNxXZbC217LGop5iT1inYuPX0LF5\n" +
            "gDNOV6FGA2MvXcxyp6M9xaZhNW8mM8s12mMYFxUoitv6qV8JwjHEH3OzJhhmNHXu\n" +
            "rEmHyTA8Axs2D9zDzFJL73miPzmrNVcC/fHhu8zCDmuaYhUa0qtL5/AoUEbR4MfU\n" +
            "7J0nmRW1LKqF3jwGlqY1LhRljI+F3jyXS344KMOl+Z7te2/y5E6JbegB34ZwOZ1x\n" +
            "fbujpM3iqHI42b4M1/g5Wzu9Lh16gO3Badpuk4sybN8hFCaWf8wiWYp8/WM4TwAA\n" +
            "vpYQkuS1k21mI+SXkWub6ebd3PekcfpGwtpu1yIPKXNn7eqohZuycAAAAAAAAA==\n" +
            "-----END PKCS7-----\n";

    public static void main(String[] args) throws Exception {
        new RegisterDeviceTest().registerDevice();
        System.exit(0);
    }

    public void checkRequest() throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(REQUEST);
        OperationCheckerDto operationDto = cmsSignedMessage.getSignedContent(OperationCheckerDto.class);
        RegisterDto registerDto = cmsSignedMessage.getSignedContent(RegisterDto.class);
        log.info("DeviceId: " + registerDto.getDeviceId() + " - SignedContent: " + cmsSignedMessage.getSignedContentStr());
    }

    public void registerDevice() throws Exception {
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(REQUEST.getBytes(), MediaType.PKCS7_SIGNED,
                CurrencyOperation.REGISTER_DEVICE.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
        log.info("status: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
        RegisterDto registerDto = (RegisterDto) responseDto.getMessage(RegisterDto.class);
        log.info("issued cert: " + registerDto.getIssuedCertificate());
        X509Certificate certificate = PEMUtils.fromPEMToX509Cert(registerDto.getIssuedCertificate().getBytes());
        log.info("issued cert: " + certificate);
    }

}