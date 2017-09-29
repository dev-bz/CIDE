package org.free.tools;

class fixit {
    private final String[] br;
    private final byte[] bytes;
    private final int[] carts;
    private final format.OpText jTextCode;

    public fixit(String fix, format.OpText op) {
        jTextCode = op;
        bytes = jTextCode.getBytes();
        carts = op.getCarts();
        br = fix.split("\n");
    }

    void startFixit() {
        if (br.length > 0) {
            //String xml="Start";
            //System.out.println("================");
            //javax.swing.text.Document jdoc=jTextCode.getDocument();
            int killYet = 0, oldPos = 0, tl = 0;
            for (String l : br)
                //if(!xml.isEmpty())xml+="\n";
                //xml+="=="+l;
                if (l.endsWith("</r>")) {
                    String[] v = l.split("'");
                    if (v.length == 5) {
                        String subSequence = v[4].substring(1, v[4].indexOf("<"));
                        int parseInt = Integer.parseInt(v[1]);
                        int parseInt1 = Integer.parseInt(v[3]);
                        parseInt1 = new String(bytes, parseInt, parseInt1).length();
                        tl = new String(bytes, oldPos, parseInt - oldPos).length() + tl;
                        oldPos = parseInt;
                        parseInt = tl + killYet;
                        //if(parseInt1>0)jTextCode.remove(parseInt,parseInt1);
                        if (subSequence.length() > 0)
                            subSequence = subSequence.replace("&#10;", "\n").replace("&#13;", "\r"); //jTextCode.insert(parseInt,subSequence);
                        jTextCode.replace(parseInt, parseInt1, subSequence);
                        for (int i = 0; i < carts.length; ++i)
                            if (carts[i] >= parseInt + parseInt1) {
                                carts[i] -= parseInt1;
                                carts[i] += subSequence.length();
                            } else if (carts[i] > parseInt)
                                carts[i] = parseInt;//+subSequence.length();
                        killYet -= parseInt1;
                        killYet += subSequence.length();
                    }
                } //xml+="\nwhile end";
            jTextCode.done("xml");
        }
    }
}
