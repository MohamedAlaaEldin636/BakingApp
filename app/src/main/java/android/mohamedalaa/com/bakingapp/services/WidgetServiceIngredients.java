package android.mohamedalaa.com.bakingapp.services;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.mohamedalaa.com.bakingapp.BaseApplication;
import android.mohamedalaa.com.bakingapp.DataRepository;
import android.mohamedalaa.com.bakingapp.R;
import android.mohamedalaa.com.bakingapp.model.Ingredients;
import android.mohamedalaa.com.bakingapp.model.Recipe;
import android.mohamedalaa.com.bakingapp.model.Steps;
import android.mohamedalaa.com.bakingapp.model.retrofit.RetrofitApiClient;
import android.mohamedalaa.com.bakingapp.model.retrofit.RetrofitApiInterface;
import android.mohamedalaa.com.bakingapp.utils.RecipeUtils;
import android.mohamedalaa.com.bakingapp.view.RecipeStepsMasterFragment;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Mohamed on 7/22/2018.
 *
 */
public class WidgetServiceIngredients extends RemoteViewsService {
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new IngredientsRemoteViewsFactory(
                getApplication(),
                this.getApplicationContext());
    }

}

class IngredientsRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Application application;
    private Context context;
    private List<Recipe> recipeList;

    IngredientsRemoteViewsFactory(Application application, Context context) {
        this.application = application;
        this.context = context;
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        // Get recipe list from
        // 1- database
        DataRepository dataRepository = ((BaseApplication) application).getRepository();
        List<Recipe> recipeList = dataRepository.getAllRecipeList();
        // 2- internet, if not in database
        if (recipeList == null || recipeList.size() == 0){
            Retrofit retrofitApiClient = RetrofitApiClient.getClient();
            RetrofitApiInterface retrofitApiInterface = retrofitApiClient
                    .create(RetrofitApiInterface.class);

            Call<List<Recipe>> call = retrofitApiInterface.getAllRecipes();
            try {
                Response<List<Recipe>> response = call.execute();

                if (response.isSuccessful()){
                    recipeList = response.body();
                    recipeList = RecipeUtils.fillImagesInsideRecipeList(recipeList);
                }
            } catch (IOException e) {
                // Maybe network failure.
            }
        }

        this.recipeList = recipeList;
    }

    @Override
    public void onDestroy() {
        recipeList = null;
    }

    @Override
    public int getCount() {
        return recipeList == null ? 0 : recipeList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // setup data in remoteViews
        Recipe recipe = recipeList.get(position);
        List<Ingredients> ingredientsList = recipe.getIngredients();
        List<Steps> stepsList = recipe.getSteps();

        String recipeName = recipe.getName();
        String ingredientsListAsString = getIngredientsListAsString(ingredientsList);

        // Setup RemoteViews
        RemoteViews remoteViews = new RemoteViews(
                context.getPackageName(), R.layout.item_ingredients_widget_list);

        // Setting header text
        remoteViews.setTextViewText(R.id.headerTextView, recipeName);
        // Setting ingredients text
        remoteViews.setTextViewText(R.id.ingredientsTextView, ingredientsListAsString);

        // perform click to launch that specific recipe ingredient
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(RecipeStepsMasterFragment.INTENT_KEY_INGREDIENTS_LIST,
                (Serializable) ingredientsList);
        fillInIntent.putExtra(RecipeStepsMasterFragment.INTENT_KEY_STEPS_LIST,
                (Serializable) stepsList);
        remoteViews.setOnClickFillInIntent(R.id.headerTextView, fillInIntent);
        remoteViews.setOnClickFillInIntent(R.id.ingredientsTextView, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        // Treat all items in the GridView the same
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // --- Private helper methods

    private String getIngredientsListAsString(List<Ingredients> ingredientsList){
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<ingredientsList.size(); i++){
            Ingredients ingredient = ingredientsList.get(i);
            builder.append(String.valueOf((i + 1)));
            builder.append(". ");
            builder.append(ingredient.getIngredient());
            builder.append(" (");
            builder.append(ingredient.getQuantity());
            builder.append("  ");
            builder.append(ingredient.getmeasure());
            builder.append(")");

            // make new line only if it is not the last ingredient.
            if (i != ingredientsList.size() - 1){
                builder.append("\n");
            }
        }

        return builder.toString();
    }

}
