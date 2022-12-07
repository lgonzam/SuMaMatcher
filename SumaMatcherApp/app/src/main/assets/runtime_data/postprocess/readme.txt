Each line is a possible lp pattern organized by region/state and then likelihood.

The parser goes through each line and tries to match
@ = any letter
# = any number
? = a skip position (can be anything, but remove it if encountered)
[A-FGZ] is just a single char position with specific letter requirements.  In this example, the regex defines characters ABCDEFGZ


SAMPLES:

es	[A]####@@
es	[A][B]####@@
es	[A][L]####@@
es	[A][V]####@@
es	[B]####@@
es	[B][A]####@@
es	[B][I]####@@
es	[B][U]####@@
es	[C]####@@
es	[C][A]####@@
es	[C][C]####@@
es	[C][S]####@@
es	[C][E]####@@
es	[C][O]####@@
es	[C][R]####@@
es	[C][U]####@@
es	[G][C]####@@
es	[G][E]####@@
es	[G][I]####@@
es	[G][R]####@@
es	[G][U]####@@
es	[H]####@@
es	[H][U]####@@
es	[P][M]####@@
es	[I][B]####@@
es	[J]####@@
es	[L]####@@
es	[L][E]####@@
es	[L][O]####@@
es	[L][U]####@@
es	[M]####@@
es	[M][A]####@@
es	[M][L]####@@
es	[M][U]####@@
es	[N][A]####@@
es	[O]####@@
es	[O][R]####@@
es	[O][U]####@@
es	[P]####@@
es	[P][O]####@@
es	[S]####@@
es	[S][A]####@@
es	[S][E]####@@
es	[S][G]####@@
es	[S][H]####@@
es	[S][O]####@@
es	[S][S]####@@
es	[T]####@@
es	[T][E]####@@
es	[T][F]####@@
es	[T][O]####@@
es	[V]####@@
es	[V][A]####@@
es	[V][I]####@@
es	[Z]####@@
es	[Z][A]####@@
es	[Z][A]####@@
es	[H]####[BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ]
es	[E]####[BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ]
es	[R]####[BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ][BCDFGHJKLMNPQRSTVWXYZ]
es	[C][M][E]####
es	[C][N][P]####@@
es	[P][G][C]####@
es	[P][M][E]####@
es	[E][T]#####
es	[E][A]#####
es	[F][N]#####
es	[M][F]#####
es	[M][M][A]#####
