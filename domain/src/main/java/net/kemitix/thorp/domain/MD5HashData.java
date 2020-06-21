package net.kemitix.thorp.domain;

public interface MD5HashData {

    class Root {
        public static final String hashString = "a3a6ac11a0eb577b81b3bb5c95cc8a6e";
        public static final MD5Hash hash = MD5Hash.create(hashString);
        public static final String base64 = "o6asEaDrV3uBs7tclcyKbg==";
        public static final RemoteKey remoteKey = RemoteKey.create("root-file");
        public static final Long size = 55L;
    }
    class Leaf {
        public static final String hashString = "208386a650bdec61cfcd7bd8dcb6b542";
        public static final MD5Hash hash = MD5Hash.create(hashString);
        public static final String base64 = "IIOGplC97GHPzXvY3La1Qg==";
        public static final RemoteKey remoteKey = RemoteKey.create("subdir/leaf-file");
        public static final Long size = 58L;
    }
    class BigFile {
        public static final String hashString = "b1ab1f7680138e6db7309200584e35d8";
        public static final MD5Hash hash = MD5Hash.create(hashString);

        public static class Part1 {
            public static final int offset = 0;
            public static final int size = 1048576;
            public static final String hashString = "39d4a9c78b9cfddf6d241a201a4ab726";
            public static final MD5Hash hash = MD5Hash.create(hashString);
        }
        public static class Part2 {
            public static final int offset = 1048576;
            public static final int size = 1048576;
            public static final String hashString = "af5876f3a3bc6e66f4ae96bb93d8dae0";
            public static final MD5Hash hash = MD5Hash.create(hashString);
        }
    }

}
