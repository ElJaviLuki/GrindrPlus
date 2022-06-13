package com.eljaviluki.grindrplus;

import static com.eljaviluki.grindrplus.Constants.GRINDR_PKG;

public class Obfuscation {
    public static class GApp {
        public static class base {
            private static final String _base = GRINDR_PKG + ".base";

            public static class Experiment {
                private static final String _experiment = _base + ".g";

                public static final String IExperimentManager = _experiment + ".b";
            }
        }

        public static class experiment {
            private static final String _experiment = GRINDR_PKG + ".experiment";

            public static final String Experiments = _experiment + ".Experiments";
            public static class Experiments_ {
                public static final String uncheckedIsEnabled_expMgr = Experiments + ".a";
            }
        }

        public static class model {
            private static final String _model = GRINDR_PKG + ".model";

            public static final String ExpiringPhotoStatusResponse = _model + ".ExpiringPhotoStatusResponse";
            public static class ExpiringPhotoStatusResponse_ {
                public static final String getTotal = "getTotal";
                public static final String getAvailable = "getAvailable";
            }

            public static final String Feature = _model + ".Feature";
            public static class Feature_ {
                public static final String isGranted = "isGranted";
                public static final String isNotGranted = "isNotGranted";
            }
        }

        public static class persistence {
            private static final String _persistence = GRINDR_PKG + ".persistence";

            public static class model {
                private static final String _model = _persistence + ".model";

                public static final String Profile = _model + ".Profile";
            }

            public static class repository {
                private static final String _repository = _persistence + ".repository";

                public static final String ChatRepo = _repository + ".ChatRepo";
                public static class ChatRepo_ {
                    public static final String checkMessageForVideoCall = "checkMessageForVideoCall";
                }
            }
        }

        public static class R {
            private static final String _R = GRINDR_PKG + ".m";

            public static final String color = _R + "$d";
            public static class color_ {
                public static final String grindr_pure_white = "I";
            }
        }

        public static class storage {
            private static final String _storage = GRINDR_PKG + ".storage";

            public static final String IUserSession = _storage + ".IUserSession";
            public static class IUserSession_ {
                public static final String hasFeature_feature = "a";

                public static final String isFree = "i";
                public static final String isNoXtraUpsell = "j";
                public static final String isXtra = "k";
                public static final String isUnlimited = "l";
            }

            public static final String UserSession = _storage + ".ai";
            public static final String UserSession2 = _storage + ".aj";
        }

        public static class ui {
            private static final String _ui = GRINDR_PKG + ".ui";

            public static class profileV2 {
                private static final String _profileV2 = _ui + ".profileV2";

                public static final String ProfileFieldsView = _profileV2 + ".ProfileFieldsView";
            }
        }

        public static class utils {
            private static final String _utils = GRINDR_PKG + ".utils";

            public static final String Styles = _utils + ".bh";
            public static class Styles_ {
                public static final String INSTANCE = "a";
                public static final String _maybe_pureWhite = "f";
            }
        }

        public static class view {
            private static final String _view = GRINDR_PKG + ".view";

            public static final String ExtendedProfileFieldView = _view + ".bv";
            public static class ExtendedProfileFieldView_ {
                public static final String setLabel = "a";
                public static final String setValue = "b";
            }
        }
    }
}
