public class audience implements movie_genre {

    public void print_type() {
        System.out.println("Action Comedy");
    }

    public void age(int x) {
        System.out.println("Age of audience is around " + x);
    }

    public static void main(String[] args) {
        audience obj = new audience();
        movie_genre aud = (movie_genre) obj;
        aud.print_type();
        aud.age(25);
    }
};