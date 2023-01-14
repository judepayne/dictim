// Compiled by ClojureScript 1.11.60 {:target :nodejs, :nodejs-rt true, :optimizations :none}
goog.provide('cljs.repl');
goog.require('cljs.core');
goog.require('cljs.spec.alpha');
goog.require('goog.string');
goog.require('goog.string.format');
cljs.repl.print_doc = (function cljs$repl$print_doc(p__9680){
var map__9681 = p__9680;
var map__9681__$1 = cljs.core.__destructure_map.call(null,map__9681);
var m = map__9681__$1;
var n = cljs.core.get.call(null,map__9681__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var nm = cljs.core.get.call(null,map__9681__$1,new cljs.core.Keyword(null,"name","name",1843675177));
cljs.core.println.call(null,"-------------------------");

cljs.core.println.call(null,(function (){var or__5045__auto__ = new cljs.core.Keyword(null,"spec","spec",347520401).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return [(function (){var temp__5804__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__5804__auto__)){
var ns = temp__5804__auto__;
return [cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns),"/"].join('');
} else {
return null;
}
})(),cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join('');
}
})());

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Protocol");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m))){
var seq__9682_9710 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__9683_9711 = null;
var count__9684_9712 = (0);
var i__9685_9713 = (0);
while(true){
if((i__9685_9713 < count__9684_9712)){
var f_9714 = cljs.core._nth.call(null,chunk__9683_9711,i__9685_9713);
cljs.core.println.call(null,"  ",f_9714);


var G__9715 = seq__9682_9710;
var G__9716 = chunk__9683_9711;
var G__9717 = count__9684_9712;
var G__9718 = (i__9685_9713 + (1));
seq__9682_9710 = G__9715;
chunk__9683_9711 = G__9716;
count__9684_9712 = G__9717;
i__9685_9713 = G__9718;
continue;
} else {
var temp__5804__auto___9719 = cljs.core.seq.call(null,seq__9682_9710);
if(temp__5804__auto___9719){
var seq__9682_9720__$1 = temp__5804__auto___9719;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__9682_9720__$1)){
var c__5568__auto___9721 = cljs.core.chunk_first.call(null,seq__9682_9720__$1);
var G__9722 = cljs.core.chunk_rest.call(null,seq__9682_9720__$1);
var G__9723 = c__5568__auto___9721;
var G__9724 = cljs.core.count.call(null,c__5568__auto___9721);
var G__9725 = (0);
seq__9682_9710 = G__9722;
chunk__9683_9711 = G__9723;
count__9684_9712 = G__9724;
i__9685_9713 = G__9725;
continue;
} else {
var f_9726 = cljs.core.first.call(null,seq__9682_9720__$1);
cljs.core.println.call(null,"  ",f_9726);


var G__9727 = cljs.core.next.call(null,seq__9682_9720__$1);
var G__9728 = null;
var G__9729 = (0);
var G__9730 = (0);
seq__9682_9710 = G__9727;
chunk__9683_9711 = G__9728;
count__9684_9712 = G__9729;
i__9685_9713 = G__9730;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_9731 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__5045__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.call(null,arglists_9731);
} else {
cljs.core.prn.call(null,((cljs.core._EQ_.call(null,new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first.call(null,arglists_9731)))?cljs.core.second.call(null,arglists_9731):arglists_9731));
}
} else {
}
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"special-form","special-form",-1326536374).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Special Form");

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.contains_QMARK_.call(null,m,new cljs.core.Keyword(null,"url","url",276297046))){
if(cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))){
return cljs.core.println.call(null,["\n  Please see http://clojure.org/",cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))].join(''));
} else {
return null;
}
} else {
return cljs.core.println.call(null,["\n  Please see http://clojure.org/special_forms#",cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Macro");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"spec","spec",347520401).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Spec");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"REPL Special Function");
} else {
}

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
var seq__9686_9732 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__9687_9733 = null;
var count__9688_9734 = (0);
var i__9689_9735 = (0);
while(true){
if((i__9689_9735 < count__9688_9734)){
var vec__9698_9736 = cljs.core._nth.call(null,chunk__9687_9733,i__9689_9735);
var name_9737 = cljs.core.nth.call(null,vec__9698_9736,(0),null);
var map__9701_9738 = cljs.core.nth.call(null,vec__9698_9736,(1),null);
var map__9701_9739__$1 = cljs.core.__destructure_map.call(null,map__9701_9738);
var doc_9740 = cljs.core.get.call(null,map__9701_9739__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_9741 = cljs.core.get.call(null,map__9701_9739__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name_9737);

cljs.core.println.call(null," ",arglists_9741);

if(cljs.core.truth_(doc_9740)){
cljs.core.println.call(null," ",doc_9740);
} else {
}


var G__9742 = seq__9686_9732;
var G__9743 = chunk__9687_9733;
var G__9744 = count__9688_9734;
var G__9745 = (i__9689_9735 + (1));
seq__9686_9732 = G__9742;
chunk__9687_9733 = G__9743;
count__9688_9734 = G__9744;
i__9689_9735 = G__9745;
continue;
} else {
var temp__5804__auto___9746 = cljs.core.seq.call(null,seq__9686_9732);
if(temp__5804__auto___9746){
var seq__9686_9747__$1 = temp__5804__auto___9746;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__9686_9747__$1)){
var c__5568__auto___9748 = cljs.core.chunk_first.call(null,seq__9686_9747__$1);
var G__9749 = cljs.core.chunk_rest.call(null,seq__9686_9747__$1);
var G__9750 = c__5568__auto___9748;
var G__9751 = cljs.core.count.call(null,c__5568__auto___9748);
var G__9752 = (0);
seq__9686_9732 = G__9749;
chunk__9687_9733 = G__9750;
count__9688_9734 = G__9751;
i__9689_9735 = G__9752;
continue;
} else {
var vec__9702_9753 = cljs.core.first.call(null,seq__9686_9747__$1);
var name_9754 = cljs.core.nth.call(null,vec__9702_9753,(0),null);
var map__9705_9755 = cljs.core.nth.call(null,vec__9702_9753,(1),null);
var map__9705_9756__$1 = cljs.core.__destructure_map.call(null,map__9705_9755);
var doc_9757 = cljs.core.get.call(null,map__9705_9756__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_9758 = cljs.core.get.call(null,map__9705_9756__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name_9754);

cljs.core.println.call(null," ",arglists_9758);

if(cljs.core.truth_(doc_9757)){
cljs.core.println.call(null," ",doc_9757);
} else {
}


var G__9759 = cljs.core.next.call(null,seq__9686_9747__$1);
var G__9760 = null;
var G__9761 = (0);
var G__9762 = (0);
seq__9686_9732 = G__9759;
chunk__9687_9733 = G__9760;
count__9688_9734 = G__9761;
i__9689_9735 = G__9762;
continue;
}
} else {
}
}
break;
}
} else {
}

if(cljs.core.truth_(n)){
var temp__5804__auto__ = cljs.spec.alpha.get_spec.call(null,cljs.core.symbol.call(null,cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.ns_name.call(null,n)),cljs.core.name.call(null,nm)));
if(cljs.core.truth_(temp__5804__auto__)){
var fnspec = temp__5804__auto__;
cljs.core.print.call(null,"Spec");

var seq__9706 = cljs.core.seq.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.Keyword(null,"ret","ret",-468222814),new cljs.core.Keyword(null,"fn","fn",-1175266204)], null));
var chunk__9707 = null;
var count__9708 = (0);
var i__9709 = (0);
while(true){
if((i__9709 < count__9708)){
var role = cljs.core._nth.call(null,chunk__9707,i__9709);
var temp__5804__auto___9763__$1 = cljs.core.get.call(null,fnspec,role);
if(cljs.core.truth_(temp__5804__auto___9763__$1)){
var spec_9764 = temp__5804__auto___9763__$1;
cljs.core.print.call(null,["\n ",cljs.core.name.call(null,role),":"].join(''),cljs.spec.alpha.describe.call(null,spec_9764));
} else {
}


var G__9765 = seq__9706;
var G__9766 = chunk__9707;
var G__9767 = count__9708;
var G__9768 = (i__9709 + (1));
seq__9706 = G__9765;
chunk__9707 = G__9766;
count__9708 = G__9767;
i__9709 = G__9768;
continue;
} else {
var temp__5804__auto____$1 = cljs.core.seq.call(null,seq__9706);
if(temp__5804__auto____$1){
var seq__9706__$1 = temp__5804__auto____$1;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__9706__$1)){
var c__5568__auto__ = cljs.core.chunk_first.call(null,seq__9706__$1);
var G__9769 = cljs.core.chunk_rest.call(null,seq__9706__$1);
var G__9770 = c__5568__auto__;
var G__9771 = cljs.core.count.call(null,c__5568__auto__);
var G__9772 = (0);
seq__9706 = G__9769;
chunk__9707 = G__9770;
count__9708 = G__9771;
i__9709 = G__9772;
continue;
} else {
var role = cljs.core.first.call(null,seq__9706__$1);
var temp__5804__auto___9773__$2 = cljs.core.get.call(null,fnspec,role);
if(cljs.core.truth_(temp__5804__auto___9773__$2)){
var spec_9774 = temp__5804__auto___9773__$2;
cljs.core.print.call(null,["\n ",cljs.core.name.call(null,role),":"].join(''),cljs.spec.alpha.describe.call(null,spec_9774));
} else {
}


var G__9775 = cljs.core.next.call(null,seq__9706__$1);
var G__9776 = null;
var G__9777 = (0);
var G__9778 = (0);
seq__9706 = G__9775;
chunk__9707 = G__9776;
count__9708 = G__9777;
i__9709 = G__9778;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
} else {
return null;
}
}
});
/**
 * Constructs a data representation for a Error with keys:
 *  :cause - root cause message
 *  :phase - error phase
 *  :via - cause chain, with cause keys:
 *           :type - exception class symbol
 *           :message - exception message
 *           :data - ex-data
 *           :at - top stack element
 *  :trace - root cause stack elements
 */
cljs.repl.Error__GT_map = (function cljs$repl$Error__GT_map(o){
var base = (function (t){
return cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),(((t instanceof cljs.core.ExceptionInfo))?new cljs.core.Symbol("cljs.core","ExceptionInfo","cljs.core/ExceptionInfo",701839050,null):(((t instanceof Error))?cljs.core.symbol.call(null,"js",t.name):null
))], null),(function (){var temp__5804__auto__ = cljs.core.ex_message.call(null,t);
if(cljs.core.truth_(temp__5804__auto__)){
var msg = temp__5804__auto__;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"message","message",-406056002),msg], null);
} else {
return null;
}
})(),(function (){var temp__5804__auto__ = cljs.core.ex_data.call(null,t);
if(cljs.core.truth_(temp__5804__auto__)){
var ed = temp__5804__auto__;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"data","data",-232669377),ed], null);
} else {
return null;
}
})());
});
var via = (function (){var via = cljs.core.PersistentVector.EMPTY;
var t = o;
while(true){
if(cljs.core.truth_(t)){
var G__9779 = cljs.core.conj.call(null,via,t);
var G__9780 = cljs.core.ex_cause.call(null,t);
via = G__9779;
t = G__9780;
continue;
} else {
return via;
}
break;
}
})();
var root = cljs.core.peek.call(null,via);
return cljs.core.merge.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"via","via",-1904457336),cljs.core.vec.call(null,cljs.core.map.call(null,base,via)),new cljs.core.Keyword(null,"trace","trace",-1082747415),null], null),(function (){var temp__5804__auto__ = cljs.core.ex_message.call(null,root);
if(cljs.core.truth_(temp__5804__auto__)){
var root_msg = temp__5804__auto__;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"cause","cause",231901252),root_msg], null);
} else {
return null;
}
})(),(function (){var temp__5804__auto__ = cljs.core.ex_data.call(null,root);
if(cljs.core.truth_(temp__5804__auto__)){
var data = temp__5804__auto__;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"data","data",-232669377),data], null);
} else {
return null;
}
})(),(function (){var temp__5804__auto__ = new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358).cljs$core$IFn$_invoke$arity$1(cljs.core.ex_data.call(null,o));
if(cljs.core.truth_(temp__5804__auto__)){
var phase = temp__5804__auto__;
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"phase","phase",575722892),phase], null);
} else {
return null;
}
})());
});
/**
 * Returns an analysis of the phase, error, cause, and location of an error that occurred
 *   based on Throwable data, as returned by Throwable->map. All attributes other than phase
 *   are optional:
 *  :clojure.error/phase - keyword phase indicator, one of:
 *    :read-source :compile-syntax-check :compilation :macro-syntax-check :macroexpansion
 *    :execution :read-eval-result :print-eval-result
 *  :clojure.error/source - file name (no path)
 *  :clojure.error/line - integer line number
 *  :clojure.error/column - integer column number
 *  :clojure.error/symbol - symbol being expanded/compiled/invoked
 *  :clojure.error/class - cause exception class symbol
 *  :clojure.error/cause - cause exception message
 *  :clojure.error/spec - explain-data for spec error
 */
cljs.repl.ex_triage = (function cljs$repl$ex_triage(datafied_throwable){
var map__9783 = datafied_throwable;
var map__9783__$1 = cljs.core.__destructure_map.call(null,map__9783);
var via = cljs.core.get.call(null,map__9783__$1,new cljs.core.Keyword(null,"via","via",-1904457336));
var trace = cljs.core.get.call(null,map__9783__$1,new cljs.core.Keyword(null,"trace","trace",-1082747415));
var phase = cljs.core.get.call(null,map__9783__$1,new cljs.core.Keyword(null,"phase","phase",575722892),new cljs.core.Keyword(null,"execution","execution",253283524));
var map__9784 = cljs.core.last.call(null,via);
var map__9784__$1 = cljs.core.__destructure_map.call(null,map__9784);
var type = cljs.core.get.call(null,map__9784__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var message = cljs.core.get.call(null,map__9784__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var data = cljs.core.get.call(null,map__9784__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var map__9785 = data;
var map__9785__$1 = cljs.core.__destructure_map.call(null,map__9785);
var problems = cljs.core.get.call(null,map__9785__$1,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814));
var fn = cljs.core.get.call(null,map__9785__$1,new cljs.core.Keyword("cljs.spec.alpha","fn","cljs.spec.alpha/fn",408600443));
var caller = cljs.core.get.call(null,map__9785__$1,new cljs.core.Keyword("cljs.spec.test.alpha","caller","cljs.spec.test.alpha/caller",-398302390));
var map__9786 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.first.call(null,via));
var map__9786__$1 = cljs.core.__destructure_map.call(null,map__9786);
var top_data = map__9786__$1;
var source = cljs.core.get.call(null,map__9786__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
return cljs.core.assoc.call(null,(function (){var G__9787 = phase;
var G__9787__$1 = (((G__9787 instanceof cljs.core.Keyword))?G__9787.fqn:null);
switch (G__9787__$1) {
case "read-source":
var map__9788 = data;
var map__9788__$1 = cljs.core.__destructure_map.call(null,map__9788);
var line = cljs.core.get.call(null,map__9788__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.call(null,map__9788__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var G__9789 = cljs.core.merge.call(null,new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.second.call(null,via)),top_data);
var G__9789__$1 = (cljs.core.truth_(source)?cljs.core.assoc.call(null,G__9789,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__9789);
var G__9789__$2 = (cljs.core.truth_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null).call(null,source))?cljs.core.dissoc.call(null,G__9789__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__9789__$1);
if(cljs.core.truth_(message)){
return cljs.core.assoc.call(null,G__9789__$2,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__9789__$2;
}

break;
case "compile-syntax-check":
case "compilation":
case "macro-syntax-check":
case "macroexpansion":
var G__9790 = top_data;
var G__9790__$1 = (cljs.core.truth_(source)?cljs.core.assoc.call(null,G__9790,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__9790);
var G__9790__$2 = (cljs.core.truth_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null).call(null,source))?cljs.core.dissoc.call(null,G__9790__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__9790__$1);
var G__9790__$3 = (cljs.core.truth_(type)?cljs.core.assoc.call(null,G__9790__$2,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__9790__$2);
var G__9790__$4 = (cljs.core.truth_(message)?cljs.core.assoc.call(null,G__9790__$3,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__9790__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.call(null,G__9790__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__9790__$4;
}

break;
case "read-eval-result":
case "print-eval-result":
var vec__9791 = cljs.core.first.call(null,trace);
var source__$1 = cljs.core.nth.call(null,vec__9791,(0),null);
var method = cljs.core.nth.call(null,vec__9791,(1),null);
var file = cljs.core.nth.call(null,vec__9791,(2),null);
var line = cljs.core.nth.call(null,vec__9791,(3),null);
var G__9794 = top_data;
var G__9794__$1 = (cljs.core.truth_(line)?cljs.core.assoc.call(null,G__9794,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),line):G__9794);
var G__9794__$2 = (cljs.core.truth_(file)?cljs.core.assoc.call(null,G__9794__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file):G__9794__$1);
var G__9794__$3 = (cljs.core.truth_((function (){var and__5043__auto__ = source__$1;
if(cljs.core.truth_(and__5043__auto__)){
return method;
} else {
return and__5043__auto__;
}
})())?cljs.core.assoc.call(null,G__9794__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null))):G__9794__$2);
var G__9794__$4 = (cljs.core.truth_(type)?cljs.core.assoc.call(null,G__9794__$3,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__9794__$3);
if(cljs.core.truth_(message)){
return cljs.core.assoc.call(null,G__9794__$4,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__9794__$4;
}

break;
case "execution":
var vec__9795 = cljs.core.first.call(null,trace);
var source__$1 = cljs.core.nth.call(null,vec__9795,(0),null);
var method = cljs.core.nth.call(null,vec__9795,(1),null);
var file = cljs.core.nth.call(null,vec__9795,(2),null);
var line = cljs.core.nth.call(null,vec__9795,(3),null);
var file__$1 = cljs.core.first.call(null,cljs.core.remove.call(null,(function (p1__9782_SHARP_){
var or__5045__auto__ = (p1__9782_SHARP_ == null);
if(or__5045__auto__){
return or__5045__auto__;
} else {
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null).call(null,p1__9782_SHARP_);
}
}),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(caller),file], null)));
var err_line = (function (){var or__5045__auto__ = new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(caller);
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return line;
}
})();
var G__9798 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type], null);
var G__9798__$1 = (cljs.core.truth_(err_line)?cljs.core.assoc.call(null,G__9798,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),err_line):G__9798);
var G__9798__$2 = (cljs.core.truth_(message)?cljs.core.assoc.call(null,G__9798__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__9798__$1);
var G__9798__$3 = (cljs.core.truth_((function (){var or__5045__auto__ = fn;
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
var and__5043__auto__ = source__$1;
if(cljs.core.truth_(and__5043__auto__)){
return method;
} else {
return and__5043__auto__;
}
}
})())?cljs.core.assoc.call(null,G__9798__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(function (){var or__5045__auto__ = fn;
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null));
}
})()):G__9798__$2);
var G__9798__$4 = (cljs.core.truth_(file__$1)?cljs.core.assoc.call(null,G__9798__$3,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file__$1):G__9798__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.call(null,G__9798__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__9798__$4;
}

break;
default:
throw (new Error(["No matching clause: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__9787__$1)].join('')));

}
})(),new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358),phase);
});
/**
 * Returns a string from exception data, as produced by ex-triage.
 *   The first line summarizes the exception phase and location.
 *   The subsequent lines describe the cause.
 */
cljs.repl.ex_str = (function cljs$repl$ex_str(p__9802){
var map__9803 = p__9802;
var map__9803__$1 = cljs.core.__destructure_map.call(null,map__9803);
var triage_data = map__9803__$1;
var phase = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358));
var source = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
var line = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var symbol = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994));
var class$ = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890));
var cause = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742));
var spec = cljs.core.get.call(null,map__9803__$1,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595));
var loc = [cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5045__auto__ = source;
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return "<cljs repl>";
}
})()),":",cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5045__auto__ = line;
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return (1);
}
})()),(cljs.core.truth_(column)?[":",cljs.core.str.cljs$core$IFn$_invoke$arity$1(column)].join(''):"")].join('');
var class_name = cljs.core.name.call(null,(function (){var or__5045__auto__ = class$;
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return "";
}
})());
var simple_class = class_name;
var cause_type = ((cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["RuntimeException",null,"Exception",null], null), null),simple_class))?"":[" (",simple_class,")"].join(''));
var format = goog.string.format;
var G__9804 = phase;
var G__9804__$1 = (((G__9804 instanceof cljs.core.Keyword))?G__9804.fqn:null);
switch (G__9804__$1) {
case "read-source":
return format.call(null,"Syntax error reading source at (%s).\n%s\n",loc,cause);

break;
case "macro-syntax-check":
return format.call(null,"Syntax error macroexpanding %sat (%s).\n%s",(cljs.core.truth_(symbol)?[cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)," "].join(''):""),loc,(cljs.core.truth_(spec)?(function (){var sb__5690__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__9805_9814 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__9806_9815 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__9807_9816 = true;
var _STAR_print_fn_STAR__temp_val__9808_9817 = (function (x__5691__auto__){
return sb__5690__auto__.append(x__5691__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__9807_9816);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__9808_9817);

try{cljs.spec.alpha.explain_out.call(null,cljs.core.update.call(null,spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.call(null,(function (p1__9800_SHARP_){
return cljs.core.dissoc.call(null,p1__9800_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__9806_9815);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__9805_9814);
}
return cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5690__auto__);
})():format.call(null,"%s\n",cause)));

break;
case "macroexpansion":
return format.call(null,"Unexpected error%s macroexpanding %sat (%s).\n%s\n",cause_type,(cljs.core.truth_(symbol)?[cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)," "].join(''):""),loc,cause);

break;
case "compile-syntax-check":
return format.call(null,"Syntax error%s compiling %sat (%s).\n%s\n",cause_type,(cljs.core.truth_(symbol)?[cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)," "].join(''):""),loc,cause);

break;
case "compilation":
return format.call(null,"Unexpected error%s compiling %sat (%s).\n%s\n",cause_type,(cljs.core.truth_(symbol)?[cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)," "].join(''):""),loc,cause);

break;
case "read-eval-result":
return format.call(null,"Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause);

break;
case "print-eval-result":
return format.call(null,"Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause);

break;
case "execution":
if(cljs.core.truth_(spec)){
return format.call(null,"Execution error - invalid arguments to %s at (%s).\n%s",symbol,loc,(function (){var sb__5690__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__9809_9818 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__9810_9819 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__9811_9820 = true;
var _STAR_print_fn_STAR__temp_val__9812_9821 = (function (x__5691__auto__){
return sb__5690__auto__.append(x__5691__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__9811_9820);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__9812_9821);

try{cljs.spec.alpha.explain_out.call(null,cljs.core.update.call(null,spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.call(null,(function (p1__9801_SHARP_){
return cljs.core.dissoc.call(null,p1__9801_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__9810_9819);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__9809_9818);
}
return cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5690__auto__);
})());
} else {
return format.call(null,"Execution error%s at %s(%s).\n%s\n",cause_type,(cljs.core.truth_(symbol)?[cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)," "].join(''):""),loc,cause);
}

break;
default:
throw (new Error(["No matching clause: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__9804__$1)].join('')));

}
});
cljs.repl.error__GT_str = (function cljs$repl$error__GT_str(error){
return cljs.repl.ex_str.call(null,cljs.repl.ex_triage.call(null,cljs.repl.Error__GT_map.call(null,error)));
});

//# sourceMappingURL=repl.js.map
