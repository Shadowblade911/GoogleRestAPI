public class ChildInfo {

    private String Id;
    private String Name;

    public ChildInfo(){ }

    public ChildInfo(String id, String name){
        this.Id = id;
        this.Name = name;
    }

    void setId(String value){
        this.Id = value;
    }


    String getId(){
        return this.Id;
    }

    void setName(String value){
        this.Name = value;
    }


    String getName(){
        return this.Name;
    }

}