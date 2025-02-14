create
class Song extends V;

create
property Song.title STRING;

create
property Song.author STRING;

create
property Song.lyrics STRING;

create
property Song.description STRING;

create
class Author extends V;
create
property Author.name STRING;
create
property Author.score INTEGER;

begin;
create
vertex Author set name="Bob Dylan", score =10;
create
vertex Author set name="Grateful Dead", score =5;
create
vertex Author set name="Lennon McCartney", score =7;
create
vertex Author set name="Chuck Berry", score =10;
create
vertex Author set name="Jack Mountain", score =4;

create
vertex Song set title="BELIEVE IT OR NOT", author="Hunter", lyrics="believe you are right";
create
vertex Song set title="ANDD WE BID YOU GOODNIGHT", author="Traditional";
create
vertex Song set title="BELIEVE IT OR NOTT", author="Hunter" ,lyrics="believe the hunter";
create
vertex Song set title="NOT FADE AWAY", author="Hardin Petty";
create
vertex Song set title="BALLAD OF FRANKIE LEE AND JUDAS PRIEST", author="Bob Dylan";
create
vertex Song set title="BETTY AND DUPREE", author="Traditional";
create
vertex Song set title="HEY BO DIDDLEY", author="Bo Diddley", lyrics = "lyrics text happy", description="desc of happiness";
create
vertex Song set title="IM A MAN", author="Spencer Davis", lyrics = "lyrics text sad", description="desc of sadness";
create
vertex Song set title="BERTHA", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="GOING DOWN THE ROAD FEELING BAD", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="MONA", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="JACK STRAW", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="JAM", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="CASEY JONES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="DEAL", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="TRUCKING", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BABY BLUE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DRUMS", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="STELLA BLUE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MOUNTAIN JAM", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="PROMISED LAND", author="Chuck Berry",lyrics = "lyrics text";
create
vertex Song set title="BEAT IT ON DOWN THE LINE", author="Jesse Fuller",lyrics = "lyrics text";
create
vertex Song set title="COLD RAIN AND SNOW", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="UNCLE JOHNS BAND", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="PLAYING IN THE BAND", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="SIDEWALKS OF NEW YORK", author="Lawlor Blake",lyrics = "lyrics text";
create
vertex Song set title="SUGAR MAGNOLIA", author="Hunter Weir",lyrics = "lyrics text";
create
vertex Song set title="ONE MORE SATURDAY NIGHT", author="Weir",lyrics = "lyrics text";
create
vertex Song set title="MISSISSIPPI HALF-STEP", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="HERE COMES SUNSHINE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="I FOUGHT THE LAW", author="Sonny Curtis",lyrics = "lyrics text";
create
vertex Song set title="LUCY IN THE SKY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WEREWOLVES OF LONDON", author="Warren Zevon",lyrics = "lyrics text";
create
vertex Song set title="BABA ORILEY", author="Pete Townshend",lyrics = "lyrics text";
create
vertex Song set title="ATTICS OF MY LIFE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THIS COULD BE THE LAST TIME", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="FOOLISH HEART", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THE WEIGHT", author="Robbie Robertson",lyrics = "lyrics text";
create
vertex Song set title="SALT LAKE CITY", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="GOOD MORNING LITTLE SCHOOL GIRL", author="Williamson",lyrics = "lyrics text";
create
vertex Song set title="GREATEST STORY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WANG DANG DOODLE", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="PICASSO MOON", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="SHAKEDOWN STREET", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LIBERTY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="US BLUES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MAGGIES FARM", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="SLOW TRAIN COMIN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BIRDSONG", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="IKO IKO", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="KNOCKING ON HEAVENS DOOR", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="FEEL LIKE A STRANGER", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="DONT EASE ME IN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="QUINN THE ESKIMO", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SUGAREE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WE BID YOU GOODNIGHT", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BOX OF RAIN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="GOOD LOVING", author="Resnick Clark",lyrics = "lyrics text";
create
vertex Song set title="SCARLET BEGONIAS", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LET THE GOOD TIMES ROLL", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BLACKBIRD", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="HEY POCKY WAY", author="Meters (Traditional)",lyrics = "lyrics text";
create
vertex Song set title="THE OTHER ONE", author="Weir",lyrics = "lyrics text";
create
vertex Song set title="REVOLUTION", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="TOUCH OF GREY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="TURN ON YOUR LOVE LIGHT", author="Scott Malone",lyrics = "lyrics text";
create
vertex Song set title="I NEED A MIRACLE", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="CHINA DOLL", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WALKING BLUES", author="Robert Johnson",lyrics = "lyrics text";
create
vertex Song set title="SHE BELONGS TO ME", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="THE FROZEN LOGGER", author="Stevens",lyrics = "lyrics text";
create
vertex Song set title="ROADRUNNER", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="HELL IN A BUCKET", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="NEW ORLEANS", author="Guida Royster",lyrics = "lyrics text";
create
vertex Song set title="MIDNIGHT HOUR", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DAY JOB", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THE MUSIC NEVER STOPPED", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="EYES OF THE WORLD", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THE WHEEL", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="CASSIDY", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="DANCIN IN THE STREETS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="RAMBLE ON ROSE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MORNING DEW", author="Bonnie Dobson",lyrics = "lyrics text";
create
vertex Song set title="HELP ON THE WAY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="NOBODYS FAULT BUT MINE", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="CAUTION", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="BROKEDOWN PALACE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="ME AND MY UNCLE", author="John Phillips",lyrics = "lyrics text";
create
vertex Song set title="BLACK PETER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="AROUND AND AROUND", author="Chuck Berry",lyrics = "lyrics text";
create
vertex Song set title="COMES A TIME", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WHARF RAT", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="HES GONE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WEATHER REPORT SUITE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="ME AND BOBBY MCGEE", author="Kristofferson Foster",lyrics = "lyrics text";
create
vertex Song set title="LAZY LIGHTNING", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="MAMA TRIED", author="Merle Haggard",lyrics = "lyrics text";
create
vertex Song set title="BIG RIVER", author="Johnny Cash",lyrics = "lyrics text";
create
vertex Song set title="ROW JIMMY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MEXICALI BLUES", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="EL PASO", author="Marty Robbins",lyrics = "lyrics text";
create
vertex Song set title="BLACK THROATED WIND", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="CUMBERLAND BLUES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="CRAZY FINGERS", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WHEN I PAINT MY MASTERPIECE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="JUST A LITTLE LIGHT", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="WE CAN RUN BUT WE CANT HIDE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SPOONFUL", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="LITTLE RED ROOSTER", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="THE SAME THING", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="MY BROTHER ESAU", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="SHIP OF FOOLS", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LOOKS LIKE RAIN", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="CHINA CAT SUNFLOWER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="NEW MINGLEWOOD BLUES", author="Noah Lewis",lyrics = "lyrics text";
create
vertex Song set title="ITS ALL OVER NOW", author="B and S Womack",lyrics = "lyrics text";
create
vertex Song set title="ESTIMATED PROPHET", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="SAMSON AND DELILAH", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="LOST SAILOR", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="SUNSHINE DAYDREAM", author="Hunter Weir",lyrics = "lyrics text";
create
vertex Song set title="BLUES FOR ALLAH", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="STANDING ON THE MOON", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="ALL ALONG THE WATCHTOWER", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="JOHNNY B GOODE", author="Chuck Berry",lyrics = "lyrics text";
create
vertex Song set title="GOOD TIME BLUES", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="ALABAMA GETAWAY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="HARD TO HANDLE", author="Redding",lyrics = "lyrics text";
create
vertex Song set title="SATISFACTION", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THROWING STONES", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="BABY WHAT YOU WANT ME TO DO", author="Jimmy Reed",lyrics = "lyrics text";
create
vertex Song set title="HOW SWEET IT IS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WHERE HAVE THE HEROES GONE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="OH BOY", author="West Tilghman Holly",lyrics = "lyrics text";
create
vertex Song set title="LADY DI", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THEY LOVE EACH OTHER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THE RACE IS ON", author="Don Rollins",lyrics = "lyrics text";
create
vertex Song set title="LONG WAY TO GO HOME", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WAY TO GO HOME", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="VICTIM OR THE CRIME", author="Graham",lyrics = "lyrics text";
create
vertex Song set title="SAINT OF CIRCUMSTANCE", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="SAMBA IN THE RAIN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="ETERNITY", author="Dixon",lyrics = "lyrics text";
create
vertex Song set title="CORINA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="YOU AINT WOMAN ENOUGH", author="Loretta Lynn",lyrics = "lyrics text";
create
vertex Song set title="FRANKLINS TOWER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="STAGGER LEE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="IT MUST HAVE BEEN THE ROSES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LOOSE LUCY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BIG BOSS MAN", author="Smith Dixon",lyrics = "lyrics text";
create
vertex Song set title="MIGHT AS WELL", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="ON THE ROAD AGAIN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="PASSENGER", author="Monk",lyrics = "lyrics text";
create
vertex Song set title="FAR FROM ME", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="ALTHEA", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LOSER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BIRD SONG", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DIRE WOLF", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="YOU WIN AGAIN", author="Hank Williams",lyrics = "lyrics text";
create
vertex Song set title="HURTS ME TOO", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="TWO SOULS IN COMMUNION", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DARK STAR", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="FRIEND OF THE DEVIL", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BROWN EYED WOMEN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="CANDYMAN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="TENNESSE JED", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="TOM THUMB BLUES", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="HIGH TIME", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="DUPREES DIAMOND BLUES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BIG RAILROAD BLUES", author="Noah Lewis",lyrics = "lyrics text";
create
vertex Song set title="TO LAY ME DOWN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LAZY RIVER", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="NEXT TIME YOU SEE ME", author="Forest Harvey",lyrics = "lyrics text";
create
vertex Song set title="CHINATOWN SHUFFLE", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="SING ME BACK HOME", author="Merle Haggard",lyrics = "lyrics text";
create
vertex Song set title="WAVE THAT FLAG", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="I KNOW YOU RIDER", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="DESOLATION ROW", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="BUILT TO LAST", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WHEN PUSH COMES TO SHOVE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="GIMME SOME LOVIN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DAY TRIPPER", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="DONT NEED LOVE", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="HULLY GULLY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="TERRAPIN STATION", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="STRONGER THAN DIRT", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SUPPLICATION", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="LET ME SING YOUR BLUES AWAY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MIND LEFT BODY JAM", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="TOP OF THE WORLD", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="STANDER ON THE MOUNTAIN", author="Hornsby",lyrics = "I stand on the mountain and I feel good";
create
vertex Song set title="WAVE TO THE WIND", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="NEW SPEEDWAY BOOGIE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LET IT GROW", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="FUNICULI FUNICULA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="VISIONS OF JOHANNA", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="EASY TO LOVE YOU", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="QUEEN JANE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SO MANY ROADS", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WEST LA FADEAWAY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DEEP ELEM BLUES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="SUNRISE", author="Donna Godchaux",lyrics = "lyrics text";
create
vertex Song set title="MISSION IN THE RAIN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="KANSAS CITY", author="Leiber Stoller",lyrics = "lyrics text";
create
vertex Song set title="STUCK INSIDE OF MOBILE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BROKEN ARROW", author="Robbie Robertson",lyrics = "lyrics text";
create
vertex Song set title="IF THE SHOE FITS", author="Charles",lyrics = "lyrics text";
create
vertex Song set title="OH BABE IT AINT NO LIE", author="Elizabeth Cotten",lyrics = "lyrics text";
create
vertex Song set title="RIPPLE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="FROM THE HEART OF ME", author="Donna Godchaux",lyrics = "lyrics text";
create
vertex Song set title="TOMORROW IS FOREVER", author="Dolly Parton",lyrics = "lyrics text";
create
vertex Song set title="MONKEY AND THE ENGINEER", author="Jesse Fuller",lyrics = "lyrics text";
create
vertex Song set title="BEEN ALL AROUND THIS WORLD", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="I JUST WANNA MAKE LOVE TO YOU", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="ROLLIN AND TUMBLIN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THAT WOULD BE SOMETHING", author="Paul McCartney",lyrics = "lyrics text";
create
vertex Song set title="SMOKESTACK LIGHTNING", author="Chester Burnette",lyrics = "lyrics text";
create
vertex Song set title="THE ELEVEN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="HEAVEN HELP THE FOOL", author="Barlow instrumental",lyrics = "lyrics text";
create
vertex Song set title="OTHER ONE JAM", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SPANISH JAM", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="CHILDREN OF THE EIGHTIES", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BYE BYE LOVE", author="F and B Bryant",lyrics = "lyrics text";
create
vertex Song set title="PEGGY O", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="JACK A ROE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DEVIL WITH A BLUE DRESS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="TONS OF STEEL", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="KEEP ON GROWING", author="Clapton Whitlock",lyrics = "lyrics text";
create
vertex Song set title="MAYBE YOU KNOW HOW I FEEL", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SUGAR SHACK", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="OLLIN ARRAGEED", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="RAIN", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="CRYPTICAL ENVELOPMENT", author="Garcia",lyrics = "lyrics text";
create
vertex Song set title="WOMEN ARE SMARTER", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="UNBROKEN CHAIN", author="Petersen",lyrics = "lyrics text";
create
vertex Song set title="ITS ALL TOO MUCH", author="George Harrison",lyrics = "lyrics text";
create
vertex Song set title="HOW LONG BLUES", author="Leroy Carr Frank Stokes",lyrics = "lyrics text";
create
vertex Song set title="CALIFORNIA EARTHQUAKE", author="Rodney Crowell",lyrics = "lyrics text";
create
vertex Song set title="ROCKIN PNEUMONIA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="EASY ANSWERS", author="Hunter Weir",lyrics = "lyrics text";
create
vertex Song set title="GENTLEMEN START YOUR ENGINES", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="DOWN IN THE BOTTOM", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="ADDAMS FAMILY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THE DAYS BETWEEN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="MAN OF PEACE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="A MIND TO GIVE UP LIVIN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BORN ON THE BAYOU", author="John Fogerty",lyrics = "lyrics text";
create
vertex Song set title="BLOW AWAY", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="REVOLUTIONARY HAMSTRUNG BLUES", author="Petersen",lyrics = "lyrics text";
create
vertex Song set title="MACK THE KNIFE", author="Brecht Wiell",lyrics = "lyrics text";
create
vertex Song set title="FIRE ON THE MOUNTAIN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="I WANT TO TELL YOU", author="George Harrison",lyrics = "lyrics text";
create
vertex Song set title="GLORIA", author="Van Morrison",lyrics = "lyrics text";
create
vertex Song set title="SPACE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="MATILDA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="CHILDHOODS END", author="Lesh",lyrics = "lyrics text";
create
vertex Song set title="GOOD GOLLY MISS MOLLY", author="Blackwell Marascalco",lyrics = "lyrics text";
create
vertex Song set title="LOUIE LOUIE", author="Richard Berry",lyrics = "lyrics text";
create
vertex Song set title="TAKE ME TO THE RIVER", author="Al Green",lyrics = "lyrics text";
create
vertex Song set title="LET IT ROCK", author="Chuck Berry",lyrics = "lyrics text";
create
vertex Song set title="HAPPY BIRTHDAY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="RUBIN AND CHERISE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="MONEY MONEY", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="AINT SUPERSTITIOUS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="VALLEY ROAD", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WILLIE AND THE HAND JIVE", author="Johnny Otis",lyrics = "lyrics text";
create
vertex Song set title="MISTER CHARLIE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="DARK HOLLOW", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="STIR IT UP", author="Bob Marley",lyrics = "lyrics text";
create
vertex Song set title="EVERY TIME YOU GO", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="GOODNIGHT IRENE", author="Leadbelly",lyrics = "lyrics text";
create
vertex Song set title="WALKIN THE DOG", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BANANA BOAT SONG", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="IT TAKES A TRAIN TO CRY", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WHY DONT WE DO IT IN THE ROAD", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="DEATH DONT HAVE NO MERCY", author="Rev Gary Davis",lyrics = "lyrics text";
create
vertex Song set title="HEY JUDE", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="CHINESE BONES", author="Robyn Hitchcock",lyrics = "lyrics text";
create
vertex Song set title="ONLY A FOOL", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="COSMIC CHARLIE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="HEART OF MINE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="BALLAD OF A THIN MAN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="CHIMES OF FREEDOM", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="TOMORROW IS A LONG TIME", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="I WILL TAKE YOU HOME", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="GET BACK", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="KING BEE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="DONT THINK TWICE ITS ALRIGHT", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THE ALHAMBRA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="WHATS GOING ON", author="Cleveland Gaye",lyrics = "lyrics text";
create
vertex Song set title="IF I HAD THE WORLD TO GIVE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MOJO", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="LOVE THE ONE YOURE WITH", author="Stephen Stills",lyrics = "lyrics text";
create
vertex Song set title="BLACK QUEEN", author="Stephen Stills",lyrics = "lyrics text";
create
vertex Song set title="SILENT WAY JAM", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="CHANTING BY THE GYOTO MONKS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="FRERE JACQUES", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="FEVER", author="Davenport Cooley",lyrics = "lyrics text";
create
vertex Song set title="BANKS OF OHIO", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="TANGLED UP IN BLUE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="LA BAMBA", author="Traditional (arr Valens)",lyrics = "lyrics text";
create
vertex Song set title="TAKE IT OFF", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="RAINY DAY WOMAN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SIMPLE TWIST OF FATE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="WATCHING THE RIVER FLOW", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="ILL BE YOUR BABY TONIGHT", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="FOREVER YOUNG", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="LUCIFERS EYES", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="WARRIORS OF THE SUN", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="WHO DO YOU LOVE", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="SAGE AND SPIRIT", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="ITS A SIN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="CLOSE ENCOUNTERS", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THATS ALRIGHT MAMA", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BIG BOY PETE", author="Harris Terry",lyrics = "lyrics text";
create
vertex Song set title="TELL MAMA", author="Etta James",lyrics = "lyrics text";
create
vertex Song set title="MARDI GRAS PARADE", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="KC MOAN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="TOMORROW NEVER KNOWS", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="GOTTA SERVE SOMEBODY", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="PROUD MARY", author="John Fogerty",lyrics = "lyrics text";
create
vertex Song set title="ROSALIE MCFALL", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="HIDEAWAY", author="Freddie King",lyrics = "lyrics text";
create
vertex Song set title="SLIPKNOT", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="LITTLE SADIE", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="STEP BACK", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="MY BABY LEFT ME", author="Arthur Cruddup",lyrics = "lyrics text";
create
vertex Song set title="LINDA LOU", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="YOUR LOVE AT HOME", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="JOHN BROWN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="I WANT YOU", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="FRANKIE LEE AND JUDAS PRIEST", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="GREEN RIVER", author="John Fogerty",lyrics = "lyrics text";
create
vertex Song set title="TORE UP OVER YOU", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="SHELTER FROM THE STORM", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="WICKED MESSENGER", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="JOEY", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="ARE YOU LONELY FOR ME", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="YOU WONT FIND ME", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BARBRY ALLEN", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="THE BOXER", author="Fabbio",lyrics = "lyrics text";
create
vertex Song set title="BAD MOON RISING", author="John Fogerty",lyrics = "lyrics text";
create
vertex Song set title="NEIGHBORHOOD GIRLS", author="Suzanne Vega",lyrics = "lyrics text";
create
vertex Song set title="ALICE D MILLIONAIRE", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="ALLIGATOR", author="Hunter Pigpen",lyrics = "lyrics text";
create
vertex Song set title="AT A SIDING", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BARBED WIRE WHIPPING PARTY", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="BLACK MUDDY RIVER", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="BORN CROSS EYED", author="Weir",lyrics = "lyrics text";
create
vertex Song set title="CANT COME DOWN", author="Garcia",lyrics = "lyrics text";
create
vertex Song set title="CLEMENTINE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="CORRINA", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="COSMIC CHARLEY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="CREAM PUFF WAR", author="Garcia",lyrics = "lyrics text";
create
vertex Song set title="DAYS BETWEEN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="DOING THAT RAG", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="DOWN SO LONG", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="THE DWARF", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="EASY WIND", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="EMPTY PAGES", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="EQUINOX", author="Lesh",lyrics = "lyrics text";
create
vertex Song set title="FRANCE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THE GOLDEN ROAD (TO UNLIMITED DEVOTION)", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="GREATEST STORY EVER TOLD", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="HOLLYWOOD CANTATA", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="KEEP ROLLING BY", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="KEEP YOUR DAY JOB", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="KING SOLOMONS MARBLES", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="LADY WITH A FAN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LAZY RIVER ROAD", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="LITTLE STAR", author="Weir",lyrics = "lyrics text";
create
vertex Song set title="MASONS CHILDREN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="MAYBE YOU KNOW", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="MINDBENDER", author="Garcia Lesh",lyrics = "lyrics text";
create
vertex Song set title="THE MONSTER", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="MOUNTAINS OF THE MOON", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="NEVER TRUST A WOMAN", author="Mydland",lyrics = "lyrics text";
create
vertex Song set title="NEW POTATO CABOOSE", author="Petersen",lyrics = "lyrics text";
create
vertex Song set title="NO LEFT TURN UNSTONED (CARDBOARD COWBOY)", author="Lesh",lyrics = "lyrics text";
create
vertex Song set title="THE ONLY TIME IS NOW", author="Garcia",lyrics = "lyrics text";
create
vertex Song set title="OPERATOR", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="OTIS ON A SHAKEDOWN CRUISE", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="PRIDE OF CUCAMONGA", author="Petersen",lyrics = "lyrics text";
create
vertex Song set title="RED", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="REUBEN AND CERISE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="ROSEMARY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="SAINT STEPHEN", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="STANDING ON THE CORNER", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="THE STRANGER (TWO SOULS IN COMMUNION)", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="TASTEBUD", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="TENNESSEE JED", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="TERRAPIN FLYER", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="TERRAPIN TRANSIT", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="THIS TIME FOREVER", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="TILL THE MORNING COMES", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="THE VALLEY ROAD", author="Hornsby",lyrics = "lyrics text happy";
create
vertex Song set title="WALK IN THE SUNSHINE", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="WE CAN RUN", author="Barlow",lyrics = "lyrics text";
create
vertex Song set title="WEATHER REPORT SUITE PRELUDE", author="instrumental",lyrics = "lyrics text";
create
vertex Song set title="WEATHER REPORT SUITE PART 1", author="Weir Andersen",lyrics = "lyrics text";
create
vertex Song set title="WEST L.A. FADEAWAY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WHATLL YOU RAISE", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="WHATS BECOME OF THE BABY", author="Hunter",lyrics = "lyrics text";
create
vertex Song set title="YOU CANT CATCH ME", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="YOU DONT HAVE TO ASK", author="Grateful Dead",lyrics = "lyrics text";
create
vertex Song set title="YOU SEE A BROKEN HEART", author="Pigpen",lyrics = "lyrics text";
create
vertex Song set title="AINT IT CRAZY (THE RUB)", author="Lightning Hopkins",lyrics = "lyrics text";
create
vertex Song set title="AINT THAT PECULIAR", author="Robinson et al",lyrics = "lyrics text";
create
vertex Song set title="ALABAMA BOUND", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="ALL I HAVE TO DO IS DREAM", author="Boudleaux Bryant",lyrics = "lyrics text";
create
vertex Song set title="ALL OF MY LOVE", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="ANY WONDER", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="ARE YOU LONELY FOR ME BABY", author="Freddie Scott",lyrics = "lyrics text";
create
vertex Song set title="BALLAD OF CASEY JONES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="BANANA BOAT SONG (DAY-O)", author="Darling Carey Arkin",lyrics = "lyrics text";
create
vertex Song set title="BANKS OF THE OHIO", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="BARBARA ALLEN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="BIG BREASA", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="BLUE MOON", author="Rodgers Hart",lyrics = "lyrics text";
create
vertex Song set title="BLUE SUEDE SHOES", author="Carl Perkins",lyrics = "lyrics text";
create
vertex Song set title="BOXER THE", author="Paul Simon",lyrics = "lyrics text";
create
vertex Song set title="BRING ME MY SHOTGUN", author="Lightning Hopkins",lyrics = "lyrics text";
create
vertex Song set title="C.C.RIDER", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="CATHYS CLOWN", author="Don and Phil Everly",lyrics = "lyrics text";
create
vertex Song set title="CHECKING UP", author="Sonny Boy Williamson",lyrics = "lyrics text";
create
vertex Song set title="CHILDREN OF THE 80S", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="COME BACK BABY", author="Lightning Hopkins",lyrics = "lyrics text";
create
vertex Song set title="COWBOY SONG", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="DANCING IN THE STREET", author="Stevenson et al",lyrics = "lyrics text";
create
vertex Song set title="DARLING COREY", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="DEAD MAN DEAD MAN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="DEAR MR FANTASY", author="Winwood et al",lyrics = "lyrics text";
create
vertex Song set title="DEATH LETTER BLUES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="DEVIL WITH THE BLUE DRESS ON", author="Long Stevenson",lyrics = "lyrics text";
create
vertex Song set title="DO YOU WANNA DANCE?", author="Bobby Freeman",lyrics = "lyrics text";
create
vertex Song set title="DONT MESS UP A GOOD THING", author="Oliver Sain",lyrics = "lyrics text";
create
vertex Song set title="DONT THINK TWICE ITS ALL RIGHT", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="DRINK UP AND GO HOME", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="EARLY MORNING RAIN", author="Gordon Lightfoot",lyrics = "lyrics text";
create
vertex Song set title="EASY RIDER", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="SAY BOSS MAN (EIGHTEEN CHILDREN)", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="EMPTY HEART", author="Jagger Richard",lyrics = "lyrics text";
create
vertex Song set title="EVERY TIME YOU GO AWAY", author="Daryl Hall",lyrics = "lyrics text";
create
vertex Song set title="FIRE IN THE CITY", author="Peter Krug",lyrics = "lyrics text";
create
vertex Song set title="THE FLOOD", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="GAMES PEOPLE PLAY", author="Joe South",lyrics = "lyrics text";
create
vertex Song set title="GANSTER OF LOVE", author="Johnny Guitar Watson",lyrics = "lyrics text";
create
vertex Song set title="GATHERING FLOWERS FOR THE MASTERS BOUQUET", author="Marvin Baumgardner",lyrics = "lyrics text";
create
vertex Song set title="GIMME SOME LOVING", author="Winwood Davis",lyrics = "lyrics text";
create
vertex Song set title="GOOD DAY SUNSHINE", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="GOOD TIMES", author="Sam Cooke",lyrics = "lyrics text";
create
vertex Song set title="GOT MY MOJO WORKING", author="Preston Foster",lyrics = "lyrics text";
create
vertex Song set title="GREEN GREEN GRASS OF HOME", author="Curly Putnam",lyrics = "lyrics text";
create
vertex Song set title="HE WAS A FRIEND OF MINE", author="Mark Spoelstra",lyrics = "lyrics text";
create
vertex Song set title="HELP ME RHONDA", author="Brian Wilson",lyrics = "lyrics text";
create
vertex Song set title="HEY LITTLE ONE", author="Bernette Vorzon",lyrics = "lyrics text";
create
vertex Song set title="HI-HEEL SNEAKERS", author="Higgenbotham",lyrics = "lyrics text";
create
vertex Song set title="HIGHWAY 61 REVISITED", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="HOOCHIE COOCHIE MAN", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="HOW SWEET IT IS (TO BE LOVED BY YOU)", author="Holland et al",lyrics = "lyrics text";
create
vertex Song set title="(BABY) HULLY GULLY", author="Smith Goldsmith",lyrics = "lyrics text";
create
vertex Song set title="I AINT SUPERSTITIOUS", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="I GOT A MIND TO GIVE UP LIVING", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="I HEARD IT THROUGH THE GRAPEVINE", author="Strong Whitfield",lyrics = "lyrics text";
create
vertex Song set title="I JUST WANT TO MAKE LOVE TO YOU", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="I KNOW ITS A SIN", author="Jimmy Reed",lyrics = "lyrics text";
create
vertex Song set title="I SECOND THAT EMOTION", author="Robinson Cleveland",lyrics = "lyrics text";
create
vertex Song set title="I WASHED MY HANDS IN MUDDY WATER", author="J Babcock",lyrics = "lyrics text";
create
vertex Song set title="ILL GO CRAZY", author="James Brown",lyrics = "lyrics text";
create
vertex Song set title="IM A HOG FOR YOU BABY", author="Leiber Stoller",lyrics = "lyrics text";
create
vertex Song set title="IM A KING BEE", author="James Moore",lyrics = "lyrics text";
create
vertex Song set title="IM A LOVING MAN", author="Clancy Carlile",lyrics = "lyrics text";
create
vertex Song set title="IVE BEEN ALL AROUND THIS WORLD", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="IVE GOT A TIGER BY THE TAIL", author="Owens Howard",lyrics = "lyrics text";
create
vertex Song set title="IVE JUST SEEN A FACE", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="IN THE PINES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="IN THE MIDNIGHT HOUR", author="Pickett Cropper",lyrics = "lyrics text";
create
vertex Song set title="IT HURTS ME TOO", author="Tampa Red",lyrics = "lyrics text";
create
vertex Song set title="IT TAKES ... A TRAIN TO CRY", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="ITS A MANS MANS MANS WORLD", author="Brown et al",lyrics = "lyrics text";
create
vertex Song set title="ITS ALL OVER NOW BABY BLUE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="ITS MY OWN FAULT", author="John Lee Hooker",lyrics = "lyrics text";
create
vertex Song set title="IVE SEEN THEM ALL", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="JACK-A-ROE", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="JOHNS OTHER", author="Papa John Creach",lyrics = "lyrics text";
create
vertex Song set title="JORDAN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="JUST LIKE TOM THUMBS BLUES", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="KATIE MAE", author="Lightning Hopkins",lyrics = "lyrics text";
create
vertex Song set title="LADY DI AND I", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="THE LAST TIME", author="Jagger Richards",lyrics = "lyrics text";
create
vertex Song set title="LEAVE YOUR LOVE AT HOME", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="LET IT BE ME", author="Curtis et al",lyrics = "lyrics text";
create
vertex Song set title="LET ME IN", author="Gene Crysler",lyrics = "lyrics text";
create
vertex Song set title="LITTLE BUNNY FOO FOO", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="LONG BLACK LIMOUSINE", author="Stovall George",lyrics = "lyrics text";
create
vertex Song set title="LONG TALL SALLY", author="Johnson et al",lyrics = "lyrics text";
create
vertex Song set title="LOOK ON YONDERS WALL", author="Arthur Cruddup",lyrics = "lyrics text";
create
vertex Song set title="LUCY IN THE SKY WITH DIAMONDS", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="LUCKY MAN", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="MAN SMART (WOMAN SMARTER)", author="Norman Span",lyrics = "lyrics text";
create
vertex Song set title="MANNISH BOY (IM A MAN)", author="Morganfield McDaniel",lyrics = "lyrics text";
create
vertex Song set title="MARRIOTT USA", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="MATILDA MATILDA", author="Harry Belafonte",lyrics = "lyrics text";
create
vertex Song set title="MEMPHIS BLUES", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="THE MIGHTY QUINN (QUINN THE ESKIMO)", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="MR TAMBOURINE MAN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="MY BABE", author="Willie Dixon",lyrics = "lyrics text";
create
vertex Song set title="NEAL CASSADY RAP", author="Neal Cassady",lyrics = "lyrics text";
create
vertex Song set title="NEIGHBOR NEIGHBOR", author="Valler Meaux",lyrics = "lyrics text";
create
vertex Song set title="ODE FOR BILLIE DEAN", author="Jorma Kaukonen",lyrics = "lyrics text";
create
vertex Song set title="OKIE FROM MUSKOGEE", author="Merle Haggard",lyrics = "lyrics text";
create
vertex Song set title="OL SLEWFOOT", author="Hausey Manney",lyrics = "lyrics text";
create
vertex Song set title="OLD OLD HOUSE", author="Jones Bynum",lyrics = "lyrics text";
create
vertex Song set title="ONE KIND FAVOR", author="Blind Lemon Jefferson",lyrics = "lyrics text";
create
vertex Song set title="ONE WAY OUT", author="James Sehorn Williamson",lyrics = "lyrics text";
create
vertex Song set title="ONE YOU LOVE THE", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="OVERSEAS STOMP (LINDBERGH HOP)", author="Jones Shade",lyrics = "lyrics text";
create
vertex Song set title="PAIN IN MY HEART", author="Naomi Neville",lyrics = "lyrics text";
create
vertex Song set title="PAPERBACK WRITER", author="Lennon McCartney",lyrics = "lyrics text";
create
vertex Song set title="PARCHMAN FARM", author="Mose Allison",lyrics = "lyrics text";
create
vertex Song set title="PEGGYO", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="PEGGY SUE", author="Holly Allison Petty",lyrics = "lyrics text";
create
vertex Song set title="PLEASE PLEASE PLEASE", author="James Brown",lyrics = "lyrics text";
create
vertex Song set title="POLLUTION", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="PRISONER BLUES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="QUEEN JANE APPROXIMATELY", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="RAILROADING ON THE GREAT DIVIDE", author="Sara Carter",lyrics = "lyrics text";
create
vertex Song set title="RAINY DAY WOMEN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="RIOT IN CELL BLOCK", author="Lieber Stoller",lyrics = "lyrics text";
create
vertex Song set title="RIP IT UP", author="Blackwell Marascaico",lyrics = "lyrics text";
create
vertex Song set title="RIVER DEEP MOUNTAIN HIGH", author="Greenwich Barry Spector",lyrics = "lyrics text";
create
vertex Song set title="(IM A) ROAD RUNNER", author="Holland et al",lyrics = "lyrics text";
create
vertex Song set title="ROBERTA", author="Leadbelly",lyrics = "lyrics text";
create
vertex Song set title="ROCKING PNEUMONIA", author="Vincent Smith",lyrics = "lyrics text";
create
vertex Song set title="ROLLING AND TUMBLING", author="Willie Newbern",lyrics = "lyrics text";
create
vertex Song set title="ROSA LEE MCFALL", author="Charlie Monroe",lyrics = "lyrics text";
create
vertex Song set title="RUN RUDOLPH RUN", author="Brodie Marks",lyrics = "lyrics text";
create
vertex Song set title="(I CANT GET NO) SATISFACTION", author="Jagger Richards",lyrics = "lyrics text";
create
vertex Song set title="SAWMILL", author="Tillis Whatley",lyrics = "lyrics text";
create
vertex Song set title="SEARCHING", author="Lieber Stoller",lyrics = "lyrics text";
create
vertex Song set title="SEASONS OF MY HEART", author="Jones Edwards",lyrics = "lyrics text";
create
vertex Song set title="SGT PEPPERS BAND", author="Joan Baez",lyrics = "lyrics text";
create
vertex Song set title="SHES MINE", author="Lightning Hopkins",lyrics = "lyrics text";
create
vertex Song set title="SICK AND TIRED", author="Bartholomew Kenner",lyrics = "lyrics text";
create
vertex Song set title="SILVER THREADS AND GOLDEN NEEDLES", author="Reynolds Rhodes",lyrics = "lyrics text";
create
vertex Song set title="SITTING ON TOP OF THE WORLD", author="Jacobs Carter",lyrics = "lyrics text";
create
vertex Song set title="SLOW BLUES", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="SLOW TRAIN", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="SO SAD (TO WATCH GOOD LOVE GO BAD)", author="Don Everley",lyrics = "lyrics text";
create
vertex Song set title="SO WHAT", author="Miles Davis",lyrics = "lyrics text";
create
vertex Song set title="SONS AND DAUGHTERS", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="STARS AND STRIPES FOREVER", author="John Philip Sousa",lyrics = "lyrics text";
create
vertex Song set title="START ME UP", author="Jagger Richard",lyrics = "lyrics text";
create
vertex Song set title="STEALING", author="Gus Cannon",lyrics = "lyrics text";
create
vertex Song set title="SWEET GEORGIA BROWN", author="Bernie Casey Pinkard",lyrics = "lyrics text";
create
vertex Song set title="SWING LOW SWEET CHARIOT", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="TAKE IT ALL OFF", author="Eva Darby",lyrics = "lyrics text";
create
vertex Song set title="TELL IT TO ME", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="THATLL BE THE DAY", author="Buddy Holly",lyrics = "lyrics text";
create
vertex Song set title="THATS ALL RIGHT MAMA", author="Arthur Cruddup",lyrics = "lyrics text";
create
vertex Song set title="THERES SOMETHING ON YOUR MIND", author="Big Jay McNeely?",lyrics = "lyrics text";
create
vertex Song set title="THINGS I USED TO DO", author="Eddie Jones",lyrics = "lyrics text";
create
vertex Song set title="THIRTY DAYS", author="Chuck Berry",lyrics = "lyrics text";
create
vertex Song set title="TIMES THEY ARE A CHANGING THE", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="TOM DOOLEY", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="TOUGH MAMA", author="Bob Dylan",lyrics = "lyrics text";
create
vertex Song set title="TWENTY SIX MILES (SANTA CATALINA)", author="Belland Larson",lyrics = "lyrics text";
create
vertex Song set title="TWIST AND SHOUT", author="Medley Russell",lyrics = "lyrics text";
create
vertex Song set title="TWO TRAINS", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="UNCLE SAMS BLUES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="VALLEY ROAD THE", author="Bruce Hornsby",lyrics = "lyrics text";
create
vertex Song set title="VIOLA LEE BLUES", author="Noah Lewis",lyrics = "lyrics text";
create
vertex Song set title="WABASH CANNONBALL", author="A.P.Carter",lyrics = "lyrics text";
create
vertex Song set title="WAKE UP LITTLE SUSIE", author="F & B Bryant",lyrics = "lyrics text";
create
vertex Song set title="WALK DOWN THE STREET", author="Unknown",lyrics = "lyrics text";
create
vertex Song set title="WALKING THE DOG", author="Rufus Thomas",lyrics = "lyrics text";
create
vertex Song set title="WATCHING THE WHEELS", author="John Lennon",lyrics = "lyrics text";
create
vertex Song set title="WHEN A MAN LOVES A WOMAN", author="Lewis Wright",lyrics = "lyrics text";
create
vertex Song set title="WHISKEY IN THE JAR", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="WHOS LOVING YOU TONIGHT", author="Jimmy Rodgers",lyrics = "lyrics text";
create
vertex Song set title="WILL THE CIRCLE BE UNBROKEN", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="WINING BOY BLUES", author="Traditional",lyrics = "lyrics text";
create
vertex Song set title="WO WOW HEY HEY", author="Bo Diddley",lyrics = "lyrics text";
create
vertex Song set title="WORKING MAN BLUES", author="Merl Haggard",lyrics = "lyrics text";
create
vertex Song set title="YOU DONT LOVE ME", author="Willie Cobb",lyrics = "lyrics text";
create
vertex Song set title="YOUNG BLOOD", author="Leiber Stoller",lyrics = "lyrics text";

commit;